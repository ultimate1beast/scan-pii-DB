package com.privsense.pii.quasiid;

import com.privsense.core.config.QuasiIdentifierConfig;
import com.privsense.core.model.*;
import com.privsense.core.repository.CorrelatedQuasiIdentifierGroupRepository;
import com.privsense.core.repository.TableInfoRepository;
import com.privsense.core.repository.SchemaInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for analyzing and detecting quasi-identifiers in database columns.
 */
@Service
public class QuasiIdentifierAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(QuasiIdentifierAnalyzer.class);
    
    private final ClusteringBasedQuasiIdentifierDetector clusteringDetector;
    private final CorrelatedQuasiIdentifierGroupRepository groupRepository;
    private final TableInfoRepository tableInfoRepository;
    private final SchemaInfoRepository schemaInfoRepository;
    private final QuasiIdentifierConfig config;

    @Autowired
    public QuasiIdentifierAnalyzer(
            ClusteringBasedQuasiIdentifierDetector clusteringDetector,
            CorrelatedQuasiIdentifierGroupRepository groupRepository,
            TableInfoRepository tableInfoRepository,
            SchemaInfoRepository schemaInfoRepository,
            QuasiIdentifierConfig config) {
        this.clusteringDetector = clusteringDetector;
        this.groupRepository = groupRepository;
        this.tableInfoRepository = tableInfoRepository;
        this.schemaInfoRepository = schemaInfoRepository;
        this.config = config;
    }

    /**
     * Main method to analyze columns for quasi-identifier detection.
     * This method coordinates the entire QI detection process.
     * 
     * @param columnDataMap Map of column info to sample data
     * @param existingResults Detection results from PII analysis
     * @param scanMetadata Metadata about the current scan
     * @return List of identified correlated quasi-identifier groups
     */
    @Transactional
    public List<CorrelatedQuasiIdentifierGroup> analyzeQuasiIdentifiers(
            Map<ColumnInfo, SampleData> columnDataMap, 
            List<DetectionResult> existingResults,
            ScanMetadata scanMetadata) {
        
        if (!config.isEnabled()) {
            logger.info("Quasi-identifier detection is disabled in configuration");
            return Collections.emptyList();
        }

        logger.info("Starting quasi-identifier analysis for {} columns", columnDataMap.size());
        
        // 1. Filter out PII and key columns
        Map<ColumnInfo, SampleData> eligibleColumns = filterEligibleColumns(columnDataMap, existingResults);
        
        if (eligibleColumns.isEmpty()) {
            logger.info("No eligible columns for quasi-identifier detection after filtering PII and keys");
            return Collections.emptyList();
        }
        
        logger.debug("After filtering PII and keys: {} eligible columns for QI detection", eligibleColumns.size());
        
        // Ensure all referenced table entities are persisted first
        persistEntityHierarchy(eligibleColumns.keySet());
        
        // 2. Detect correlated column groups
        List<CorrelatedQuasiIdentifierGroup> correlatedGroups =
                clusteringDetector.detectCorrelatedColumns(eligibleColumns);
        
        if (correlatedGroups.isEmpty()) {
            logger.info("No correlated quasi-identifier groups detected");
            return Collections.emptyList();
        }
        
        // 3. Set scan metadata for all groups
        for (CorrelatedQuasiIdentifierGroup group : correlatedGroups) {
            group.setScanMetadata(scanMetadata);
            group.setCreatedAt(LocalDateTime.now());
            
            // 4. Calculate singleton combinations (simple estimation for demonstration)
            group.setSingletonCombinations((int)(group.getDistinctCombinations() * 0.2));
            
            // 5. Save to database
            groupRepository.save(group);
            
            logger.debug("Saved quasi-identifier group: {} with {} columns", 
                    group.getGroupName(), group.getColumns().size());
        }
        
        logger.info("Completed quasi-identifier analysis. Found {} groups", correlatedGroups.size());
        
        return correlatedGroups;
    }
    
    /**
     * Ensures that all TableInfo and SchemaInfo entities are properly persisted
     * before attempting to save CorrelatedQuasiIdentifierGroup objects.
     * This prevents the "unsaved transient instance" Hibernate error.
     */
    private void persistEntityHierarchy(Set<ColumnInfo> columns) {
        // Track already processed tables and schemas to avoid redundant saves
        Set<UUID> processedTableIds = new HashSet<>();
        Set<UUID> processedSchemaIds = new HashSet<>();

        for (ColumnInfo column : columns) {
            TableInfo table = column.getTable();
            if (table != null && table.getId() != null && !processedTableIds.contains(table.getId())) {
                // Persist schema first if needed
                SchemaInfo schema = table.getSchema();
                if (schema != null && schema.getId() != null && !processedSchemaIds.contains(schema.getId())) {
                    try {
                        schemaInfoRepository.save(schema);
                        processedSchemaIds.add(schema.getId());
                    } catch (Exception e) {
                        logger.debug("Schema already persisted: {}", schema.getSchemaName());
                        // Schema already exists, continue
                    }
                }
                
                // Now persist the table
                try {
                    tableInfoRepository.save(table);
                    processedTableIds.add(table.getId());
                } catch (Exception e) {
                    logger.debug("Table already persisted: {}", table.getTableName());
                    // Table already exists, continue
                }
            }
        }
    }
    
    /**
     * Filters out columns that aren't eligible for quasi-identifier analysis.
     * Excludes PII columns and key columns (primary keys, foreign keys).
     */
    private Map<ColumnInfo, SampleData> filterEligibleColumns(
            Map<ColumnInfo, SampleData> columnDataMap, 
            List<DetectionResult> existingResults) {
            
        // 1. Filter out columns already identified as PII
        Set<ColumnInfo> piiColumns = existingResults.stream()
                .filter(DetectionResult::hasPii)
                .map(DetectionResult::getColumnInfo)
                .collect(Collectors.toSet());
        
        // 2. Build the filtered map
        return columnDataMap.entrySet().stream()
                .filter(entry -> !piiColumns.contains(entry.getKey()))
                .filter(entry -> !entry.getKey().isPrimaryKey())
                .filter(entry -> !isReferencedByForeignKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Determines if a column is part of a foreign key relationship.
     */
    private boolean isReferencedByForeignKey(ColumnInfo columnInfo) {
        if (columnInfo.getTable() == null) return false;
        
        // Check if column is part of a foreign key relationship (imported or exported)
        return columnInfo.getTable().getImportedRelationships().stream()
                .anyMatch(rel -> rel.getTargetColumn() != null && rel.getTargetColumn().equals(columnInfo)) ||
                columnInfo.getTable().getExportedRelationships().stream()
                .anyMatch(rel -> rel.getSourceColumn() != null && rel.getSourceColumn().equals(columnInfo));
    }
    
    /**
     * Updates detection results with quasi-identifier information from the identified groups.
     * 
     * @param results Detection results to update
     * @param groups Identified quasi-identifier groups
     */
    public void updateResultsWithQuasiIdentifiers(List<DetectionResult> results, 
                                                List<CorrelatedQuasiIdentifierGroup> groups) {
                                                    
        if (groups == null || groups.isEmpty()) {
            return;
        }
        
        // Create a map of columns to their groups for efficient lookup
        Map<ColumnInfo, CorrelatedQuasiIdentifierGroup> columnToGroup = new HashMap<>();
        for (CorrelatedQuasiIdentifierGroup group : groups) {
            for (QuasiIdentifierColumnMapping mapping : group.getColumns()) {
                columnToGroup.put(mapping.getColumn(), group);
            }
        }
        
        // Update each detection result
        for (DetectionResult result : results) {
            ColumnInfo columnInfo = result.getColumnInfo();
            CorrelatedQuasiIdentifierGroup group = columnToGroup.get(columnInfo);
            
            if (group != null) {
                // Set quasi-identifier flag and risk score
                result.setQuasiIdentifier(true);
                result.setQuasiIdentifierRiskScore(group.getReIdentificationRiskScore());
                
                // Set clustering method - this is the key change to include ML info
                result.setClusteringMethod(group.getClusteringMethod());
                
                // Set correlated columns (excluding self)
                List<String> correlatedColumns = group.getColumns().stream()
                        .map(mapping -> mapping.getColumn().getColumnName())
                        .filter(name -> !name.equals(columnInfo.getColumnName()))
                        .collect(Collectors.toList());
                        
                result.setCorrelatedColumns(correlatedColumns);
                
                logger.debug("Updated detection result for {} marking as quasi-identifier with risk score {} using method {}",
                    columnInfo.getColumnName(), group.getReIdentificationRiskScore(), group.getClusteringMethod());
            }
        }
    }
}