package com.privsense.api.service.impl;

import com.privsense.core.model.*;
import com.privsense.core.repository.ColumnInfoRepository;
import com.privsense.core.repository.DetectionResultRepository;
import com.privsense.core.repository.PiiCandidateRepository;
import com.privsense.core.repository.RelationshipInfoRepository;
import com.privsense.core.repository.ScanMetadataRepository;
import com.privsense.core.repository.TableInfoRepository;
import com.privsense.core.repository.SchemaInfoRepository;
import com.privsense.core.service.ScanPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service implementation for persisting and retrieving scan results.
 */
@Service
public class ScanPersistenceServiceImpl implements ScanPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(ScanPersistenceServiceImpl.class);

    private final ScanMetadataRepository scanRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final PiiCandidateRepository piiCandidateRepository;
    private final ColumnInfoRepository columnInfoRepository;
    private final TableInfoRepository tableInfoRepository;
    private final RelationshipInfoRepository relationshipInfoRepository;
    private final SchemaInfoRepository schemaInfoRepository;

    @Autowired
    public ScanPersistenceServiceImpl(
            ScanMetadataRepository scanRepository,
            DetectionResultRepository detectionResultRepository,
            PiiCandidateRepository piiCandidateRepository,
            ColumnInfoRepository columnInfoRepository,
            TableInfoRepository tableInfoRepository,
            RelationshipInfoRepository relationshipInfoRepository,
            SchemaInfoRepository schemaInfoRepository) {
        this.scanRepository = scanRepository;
        this.detectionResultRepository = detectionResultRepository;
        this.piiCandidateRepository = piiCandidateRepository;
        this.columnInfoRepository = columnInfoRepository;
        this.tableInfoRepository = tableInfoRepository;
        this.relationshipInfoRepository = relationshipInfoRepository;
        this.schemaInfoRepository = schemaInfoRepository;
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
        logger.info("Created new scan with ID: {}", savedScan.getId());
        
        return savedScan;
    }

    @Override
    @Transactional
    public void updateScanStatus(UUID scanId, ScanMetadata.ScanStatus status) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            scan.setStatus(status);
            scanRepository.save(scan);
            logger.debug("Updated scan status for scan ID {}: {}", scanId, status);
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
            
            // First, collect all entities needed for proper saving
            Set<SchemaInfo> schemaInfos = new HashSet<>();
            Set<TableInfo> tableInfos = new HashSet<>();
            Set<ColumnInfo> columnInfos = new HashSet<>();
            Set<RelationshipInfo> relationshipInfos = new HashSet<>();
            
            for (DetectionResult result : results) {
                ColumnInfo column = result.getColumnInfo();
                columnInfos.add(column);
                
                // Add the table of this column
                if (column.getTable() != null) {
                    TableInfo table = column.getTable();
                    tableInfos.add(table);
                    
                    // Add schema if present
                    if (table.getSchema() != null) {
                        table.getSchema().setScanId(scanId);
                        schemaInfos.add(table.getSchema());
                    }
                    
                    // Add relationships if present
                    if (table.getAllRelationships() != null) {
                        for (RelationshipInfo relationship : table.getAllRelationships()) {
                            relationshipInfos.add(relationship);
                            
                            // Also add all columns referenced by the relationship
                            if (relationship.getSourceColumn() != null) {
                                columnInfos.add(relationship.getSourceColumn());
                                if (relationship.getSourceColumn().getTable() != null) {
                                    tableInfos.add(relationship.getSourceColumn().getTable());
                                }
                            }
                            if (relationship.getTargetColumn() != null) {
                                columnInfos.add(relationship.getTargetColumn());
                                if (relationship.getTargetColumn().getTable() != null) {
                                    tableInfos.add(relationship.getTargetColumn().getTable());
                                }
                            }
                        }
                    }
                }
                
                // Also collect ColumnInfo from PiiCandidate objects and their tables
                if (result.getCandidates() != null) {
                    for (PiiCandidate candidate : result.getCandidates()) {
                        ColumnInfo candidateColumn = candidate.getColumnInfo();
                        columnInfos.add(candidateColumn);
                        
                        // Add the table of this candidate's column
                        if (candidateColumn.getTable() != null) {
                            TableInfo table = candidateColumn.getTable();
                            tableInfos.add(table);
                            
                            // Add schema if present
                            if (table.getSchema() != null) {
                                table.getSchema().setScanId(scanId);
                                schemaInfos.add(table.getSchema());
                            }
                        }
                    }
                }
            }
            
            // First, save all SchemaInfo objects
            if (!schemaInfos.isEmpty()) {
                logger.debug("Saving {} schema info objects for scan ID {}", schemaInfos.size(), scanId);
                for (SchemaInfo schema : schemaInfos) {
                    if (schema.getId() == null) { // Only save if it's a new schema
                        schemaInfoRepository.save(schema);
                    }
                }
                schemaInfoRepository.flush(); // Ensure schemas are saved to the database
            }
            
            // Save all TableInfo objects next, now that their schemas are saved
            tableInfoRepository.saveAll(tableInfos);
            logger.debug("Saved {} table info objects for scan ID {}", tableInfos.size(), scanId);
            
            // Save all ColumnInfo objects next, now that their tables are saved
            columnInfoRepository.saveAll(columnInfos);
            logger.debug("Saved {} column info objects for scan ID {}", columnInfos.size(), scanId);
            
            // Save all RelationshipInfo objects now that columns and tables are saved
            if (!relationshipInfos.isEmpty()) {
                relationshipInfoRepository.saveAll(relationshipInfos);
                logger.debug("Saved {} relationship info objects for scan ID {}", relationshipInfos.size(), scanId);
            }
            
            // Link detection results to the scan
            for (DetectionResult result : results) {
                result.setScanMetadata(scan);
                scan.addDetectionResult(result);
            }
            
            // Save everything
            scanRepository.save(scan);
            logger.info("Saved scan results for scan ID {}: {} columns scanned, {} PII columns found", 
                    scanId, results.size(), scan.getTotalPiiColumnsFound());
        });
    }

    @Override
    @Transactional
    public void completeScan(UUID scanId) {
        scanRepository.findById(scanId).ifPresent(scan -> {
            scan.setEndTime(Instant.now());
            scan.setStatus(ScanMetadata.ScanStatus.COMPLETED);
            scanRepository.save(scan);
            logger.info("Completed scan ID: {}", scanId);
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
            logger.error("Scan ID {} failed: {}", scanId, errorMessage);
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
    public List<DetectionResult> getDetectionResultsByScanId(UUID scanId) {
        return detectionResultRepository.findByScanMetadataId(scanId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionResult> getPiiResultsByScanId(UUID scanId) {
        return detectionResultRepository.findPiiResultsByScanId(scanId);
    }
}