package com.privsense.api.service.impl;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages entity relationships for database objects.
 * This utility class helps maintain referential integrity when persisting database objects.
 */
@Component
public class EntityRelationshipManager {

    /**
     * Extracts related entities from detection results for persistence.
     * 
     * @param results The detection results to process
     * @param scanId The scan ID these results belong to
     * @param schemaInfos Output set to collect schema info objects
     * @param tableInfos Output set to collect table info objects
     * @param columnInfos Output set to collect column info objects
     * @param relationshipInfos Output set to collect relationship info objects
     */
    public void extractRelatedEntities(
            List<DetectionResult> results, 
            UUID scanId,
            Set<SchemaInfo> schemaInfos,
            Set<TableInfo> tableInfos,
            Set<ColumnInfo> columnInfos,
            Set<RelationshipInfo> relationshipInfos) {
            
        // Process each detection result to extract all entities
        for (DetectionResult result : results) {
            // Extract column, table, schema
            ColumnInfo column = result.getColumnInfo();
            if (column != null) {
                columnInfos.add(column);
                
                TableInfo table = column.getTable();
                if (table != null) {
                    tableInfos.add(table);
                    
                    SchemaInfo schema = table.getSchema();
                    if (schema != null) {
                        schemaInfos.add(schema);
                    }
                    
                    // Extract relationships from the table
                    extractRelationships(table, relationshipInfos);
                }
            }
        }
    }
    
    /**
     * Helper method to extract relationships from a table entity.
     */
    private void extractRelationships(TableInfo table, Set<RelationshipInfo> relationshipInfos) {
        // Get imported (foreign key) relationships
        if (table.getImportedRelationships() != null) {
            relationshipInfos.addAll(table.getImportedRelationships());
        }
        
        // Get exported (referenced by foreign keys) relationships
        if (table.getExportedRelationships() != null) {
            relationshipInfos.addAll(table.getExportedRelationships());
        }
    }
}