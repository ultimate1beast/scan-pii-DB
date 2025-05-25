package com.privsense.api.service.impl;

import com.privsense.core.model.*;
import com.privsense.core.repository.*;
import com.privsense.core.service.ScanPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for persisting and retrieving scan results.
 */
@Service
@Slf4j
public class ScanPersistenceServiceImpl implements ScanPersistenceService {

    private final ScanMetadataRepository scanRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final ColumnInfoRepository columnInfoRepository;
    private final TableInfoRepository tableInfoRepository;
    private final RelationshipInfoRepository relationshipInfoRepository;
    private final SchemaInfoRepository schemaInfoRepository;
    private final EntityRelationshipManager entityRelationshipManager;
    private final ComplianceReportRepository complianceReportRepository;

    public ScanPersistenceServiceImpl(
            ScanMetadataRepository scanRepository,
            DetectionResultRepository detectionResultRepository,
            ColumnInfoRepository columnInfoRepository,
            TableInfoRepository tableInfoRepository,
            RelationshipInfoRepository relationshipInfoRepository,
            SchemaInfoRepository schemaInfoRepository,
            EntityRelationshipManager entityRelationshipManager,
            ComplianceReportRepository complianceReportRepository) {
        this.scanRepository = scanRepository;
        this.detectionResultRepository = detectionResultRepository;
        this.columnInfoRepository = columnInfoRepository;
        this.tableInfoRepository = tableInfoRepository;
        this.relationshipInfoRepository = relationshipInfoRepository;
        this.schemaInfoRepository = schemaInfoRepository;
        this.entityRelationshipManager = entityRelationshipManager;
        this.complianceReportRepository = complianceReportRepository;
    }

    @Override
    @Transactional
    public ScanMetadata save(ScanMetadata scanMetadata) {
        return scanRepository.save(scanMetadata);
    }

    @Override
    @Transactional
    public ScanMetadata createScan(UUID connectionId, String databaseName, String databaseProductName, String databaseProductVersion) {
        ScanMetadata scan = new ScanMetadata();
        scan.setConnectionId(connectionId);
        scan.setStartTime(Instant.now());
        scan.setStatus(ScanMetadata.ScanStatus.PENDING);
        scan.setDatabaseName(databaseName);
        scan.setDatabaseProductName(databaseProductName);
        scan.setDatabaseProductVersion(databaseProductVersion);
        
        ScanMetadata savedScan = scanRepository.save(scan);
        log.info("Created new scan with ID: {}", savedScan.getId());
        
        return savedScan;
    }

    @Override
    @Transactional
    public void updateScanStatus(UUID scanId, ScanMetadata.ScanStatus status) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            scan.setStatus(status);
            scanRepository.save(scan);
            log.debug("Updated scan status for scan ID {}: {}", scanId, status);
        });
    }

    @Override
    @Transactional
    public void saveScanResults(UUID scanId, List<DetectionResult> results) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            // Update scan with final statistics
            scan.setTotalColumnsScanned(results.size());
            scan.setTotalPiiColumnsFound((int)results.stream()
                    .filter(DetectionResult::hasPii)
                    .count());
            
            // Extract relationships between entities
            Set<SchemaInfo> schemaInfos = new HashSet<>();
            Set<TableInfo> tableInfos = new HashSet<>();
            Set<ColumnInfo> columnInfos = new HashSet<>();
            Set<RelationshipInfo> relationshipInfos = new HashSet<>();
            
            // Extract all related entities using the relationship manager
            entityRelationshipManager.extractRelatedEntities(
                results, scanId, schemaInfos, tableInfos, columnInfos, relationshipInfos);
                
            // Save all entities in the correct order to maintain referential integrity
            saveEntities(schemaInfos, tableInfos, columnInfos, relationshipInfos);
            
            // Link detection results to the scan
            for (DetectionResult result : results) {
                result.setScanMetadata(scan);
                scan.addDetectionResult(result);
            }
            
            // Save everything
            scanRepository.save(scan);
            log.info("Saved scan results for scan ID {}: {} columns scanned, {} PII columns found", 
                    scanId, results.size(), scan.getTotalPiiColumnsFound());
        });
    }

    @Override
    @Transactional
    public void saveReport(UUID scanId, ComplianceReport report) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            // Set the scan reference in the report
            report.setScanId(scanId);
            
            // Save the report
            complianceReportRepository.save(report);
            log.info("Saved compliance report for scan ID: {}", scanId);
        });
    }

    /**
     * Saves entities in order to maintain referential integrity.
     */
    private void saveEntities(
            Set<SchemaInfo> schemaInfos,
            Set<TableInfo> tableInfos, 
            Set<ColumnInfo> columnInfos, 
            Set<RelationshipInfo> relationshipInfos) {
            
        // First, save all SchemaInfo objects
        if (!schemaInfos.isEmpty()) {
            log.debug("Saving {} schema info objects", schemaInfos.size());
            for (SchemaInfo schema : schemaInfos) {
                if (schema.getId() == null) { // Only save if it's a new schema
                    schemaInfoRepository.save(schema);
                }
            }
            schemaInfoRepository.flush(); // Ensure schemas are saved to the database
        }
        
        // Save all TableInfo objects next, now that their schemas are saved
        tableInfoRepository.saveAll(tableInfos);
        log.debug("Saved {} table info objects", tableInfos.size());
        
        // Save all ColumnInfo objects next, now that their tables are saved
        columnInfoRepository.saveAll(columnInfos);
        log.debug("Saved {} column info objects", columnInfos.size());
        
        // Save all RelationshipInfo objects now that columns and tables are saved
        if (!relationshipInfos.isEmpty()) {
            relationshipInfoRepository.saveAll(relationshipInfos);
            log.debug("Saved {} relationship info objects", relationshipInfos.size());
        }
    }

    @Override
    @Transactional
    public void completeScan(UUID scanId) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            scan.setEndTime(Instant.now());
            scan.setStatus(ScanMetadata.ScanStatus.COMPLETED);
            scanRepository.save(scan);
            log.info("Completed scan ID: {}", scanId);
        });
    }

    @Override
    @Transactional
    public void failScan(UUID scanId, String errorMessage) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            scan.setEndTime(Instant.now());
            scan.setStatus(ScanMetadata.ScanStatus.FAILED);
            scan.setErrorMessage(errorMessage);
            scanRepository.save(scan);
            log.error("Scan ID {} failed: {}", scanId, errorMessage);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScanMetadata> getScanById(UUID scanId) {
        return scanRepository.findById(scanId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getScansByConnectionId(UUID connectionId) {
        return scanRepository.findByConnectionId(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getScansByStatus(ScanMetadata.ScanStatus status) {
        return scanRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getScansByStatusAndConnectionId(ScanMetadata.ScanStatus status, UUID connectionId) {
        // Filter by both status and connection ID
        return scanRepository.findAll().stream()
                .filter(scan -> scan.getStatus() == status && scan.getConnectionId().equals(connectionId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getPagedScans(int page, int size) {
        // Create a pageable request with sorting by most recent first
        Pageable pageable = PageRequest.of(
            page, 
            size, 
            Sort.by(Sort.Direction.DESC, "startTime")
        );
        
        // Use the Spring Data Pageable interface to get a page of results
        return scanRepository.findAllPaged(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAllScans() {
        return scanRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionResult> getDetectionResultsByScanId(UUID scanId) {
        return detectionResultRepository.findByScanMetadataId(scanId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionResult> getPiiResultsByScanId(UUID scanId) {
        return detectionResultRepository.findPiiResultsByScanId(scanId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getAllScans() {
        List<ScanMetadata> scans = scanRepository.findAll();
        log.debug("Retrieved {} scans from the database", scans.size());
        return scans;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getScansByTimeRange(Instant startTime, Instant endTime) {
        log.debug("Getting scans between {} and {}", startTime, endTime);
        return getAllScans().stream()
                .filter(scan -> {
                    Instant scanTime = scan.getStartTime();
                    return scanTime != null && !scanTime.isBefore(startTime) && !scanTime.isAfter(endTime);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ScanMetadata> getRecentScans(int limit) {
        log.debug("Getting {} most recent scans", limit);
        // Get all scans and sort by start time in descending order
        return getAllScans().stream()
                .sorted((s1, s2) -> {
                    if (s1.getStartTime() == null) return 1;
                    if (s2.getStartTime() == null) return -1;
                    return s2.getStartTime().compareTo(s1.getStartTime());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}