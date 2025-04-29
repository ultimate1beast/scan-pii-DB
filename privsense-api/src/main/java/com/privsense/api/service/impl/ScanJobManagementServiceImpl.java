package com.privsense.api.service.impl;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.mapper.DtoMapper;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.ScanMetadata;
import com.privsense.core.repository.ConnectionRepository;
import com.privsense.core.service.ScanJobManagementService;
import com.privsense.core.service.ScanPersistenceService;
import com.privsense.core.service.ScanExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;


/**
 * Implementation of ScanJobManagementService that handles scan job lifecycle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanJobManagementServiceImpl implements ScanJobManagementService {

    private final ConnectionRepository connectionRepository;
    private final ScanPersistenceService scanPersistenceService;
    private final DtoMapper dtoMapper;
    private final ScanExecutionService scanExecutionService;

    /**
     * Enum representing the possible states of a scan job.
     */
    public enum JobState {
        PENDING,
        EXTRACTING_METADATA,
        SAMPLING,
        DETECTING_PII,
        GENERATING_REPORT,
        COMPLETED,
        FAILED
    }

    /**
     * Class representing the status of a scan job.
     */
    public static class JobStatus {
        private final UUID jobId;
        private final UUID connectionId;
        private JobState state;
        private final LocalDateTime startTime;
        private LocalDateTime lastUpdateTime;
        private String errorMessage;

        public JobStatus(UUID jobId, UUID connectionId) {
            this.jobId = jobId;
            this.connectionId = connectionId;
            this.state = JobState.PENDING;
            this.startTime = LocalDateTime.now();
            this.lastUpdateTime = LocalDateTime.now();
        }

        public UUID getJobId() {
            return jobId;
        }

        public UUID getConnectionId() {
            return connectionId;
        }

        public JobState getState() {
            return state;
        }

        public void setState(JobState state) {
            this.state = state;
            this.lastUpdateTime = LocalDateTime.now();
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    @Override
    public UUID submitScanJob(Object requestObj) {
        ScanRequest scanRequest = (ScanRequest) requestObj;
        
        // Validate connection ID exists
        if (!connectionRepository.existsById(scanRequest.getConnectionId())) {
            throw new IllegalArgumentException("Connection ID not found: " + scanRequest.getConnectionId());
        }

        // Create initial scan record in the database
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(scanRequest.getConnectionId())
            .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + scanRequest.getConnectionId()));
            
        ScanMetadata scanMetadata = scanPersistenceService.createScan(
            scanRequest.getConnectionId(), 
            connectionInfo.getDatabaseName(),
            "", // We don't have database product name in the connection info
            ""  // We don't have database product version in the connection info
        );
        
        UUID jobId = scanMetadata.getId();
        
        // Start the asynchronous scan process
        scanExecutionService.executeScanAsync(jobId, scanRequest);

        return jobId;
    }

    @Override
    public Object getJobStatus(UUID jobId) {
        // Get from the database
        Optional<ScanMetadata> scanOpt = scanPersistenceService.getScanById(jobId);
        if (!scanOpt.isPresent()) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        ScanMetadata scan = scanOpt.get();
        JobStatus status = new JobStatus(scan.getId(), scan.getConnectionId());
        
        // Convert database status to in-memory status
        switch (scan.getStatus()) {
            case PENDING:
                status.setState(JobState.PENDING);
                break;
            case EXTRACTING_METADATA:
                status.setState(JobState.EXTRACTING_METADATA);
                break;
            case SAMPLING:
                status.setState(JobState.SAMPLING);
                break;
            case DETECTING_PII:
                status.setState(JobState.DETECTING_PII);
                break;
            case GENERATING_REPORT:
                status.setState(JobState.GENERATING_REPORT);
                break;
            case COMPLETED:
                status.setState(JobState.COMPLETED);
                break;
            case FAILED:
                status.setState(JobState.FAILED);
                status.setErrorMessage(scan.getErrorMessage());
                break;
            default:
                status.setState(JobState.PENDING);
        }
        
        // Use DtoMapper to convert JobStatus to ScanJobResponse
        ScanJobResponse response = dtoMapper.fromJobStatus(status);
        
        if (status.getState() == JobState.COMPLETED) {
            log.debug("Job {} is marked as completed", jobId);
        }
        
        return response;
    }

    @Override
    public void cancelScan(UUID jobId) {
        Optional<ScanMetadata> scanOpt = scanPersistenceService.getScanById(jobId);
        if (!scanOpt.isPresent()) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        ScanMetadata scan = scanOpt.get();
        
        if (scan.getStatus() == ScanMetadata.ScanStatus.COMPLETED || 
            scan.getStatus() == ScanMetadata.ScanStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel job that is already " + scan.getStatus());
        }

        scanPersistenceService.failScan(jobId, "Job cancelled by user request");
        log.info("Job {}: Cancelled by user request", jobId);
    }

    @Override
    public List<Object> getAllJobs() {
        // Get all scans from the database
        List<ScanMetadata> scans = scanPersistenceService.getAllScans();
        
        // Convert to JobStatus objects
        List<JobStatus> statusList = scans.stream()
            .map(scan -> {
                JobStatus status = new JobStatus(scan.getId(), scan.getConnectionId());
                
                // Convert database status to in-memory status
                switch (scan.getStatus()) {
                    case PENDING:
                        status.setState(JobState.PENDING);
                        break;
                    case EXTRACTING_METADATA:
                        status.setState(JobState.EXTRACTING_METADATA);
                        break;
                    case SAMPLING:
                        status.setState(JobState.SAMPLING);
                        break;
                    case DETECTING_PII:
                        status.setState(JobState.DETECTING_PII);
                        break;
                    case GENERATING_REPORT:
                        status.setState(JobState.GENERATING_REPORT);
                        break;
                    case COMPLETED:
                        status.setState(JobState.COMPLETED);
                        break;
                    case FAILED:
                        status.setState(JobState.FAILED);
                        status.setErrorMessage(scan.getErrorMessage());
                        break;
                    default:
                        status.setState(JobState.PENDING);
                }
                return status;
            })
            .collect(Collectors.toList());
        
        // Use DtoMapper to convert JobStatus list to ScanJobResponse list
        List<ScanJobResponse> responseList = statusList.stream()
            .map(dtoMapper::fromJobStatus)
            .collect(Collectors.toList());
            
        log.debug("Retrieved {} scan jobs from the database", responseList.size());
        return new ArrayList<>(responseList); // Convert to List<Object> by creating a new ArrayList
    }
}