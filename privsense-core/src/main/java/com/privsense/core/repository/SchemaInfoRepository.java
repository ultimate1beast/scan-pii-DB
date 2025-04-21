package com.privsense.core.repository;

import com.privsense.core.model.SchemaInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing SchemaInfo entities.
 */
public interface SchemaInfoRepository {
    
    /**
     * Save a schema info entity.
     *
     * @param schemaInfo The schema info to save
     * @return The saved entity
     */
    SchemaInfo save(SchemaInfo schemaInfo);
    
    /**
     * Save a collection of schema info entities.
     *
     * @param schemaInfos The collection of schema infos to save
     * @return The saved entities
     */
    Iterable<SchemaInfo> saveAll(Collection<SchemaInfo> schemaInfos);
    
    /**
     * Find a schema info by its ID.
     *
     * @param id The ID of the schema info
     * @return Optional containing the schema info if found
     */
    Optional<SchemaInfo> findById(UUID id);
    
    /**
     * Find all schema info entities for a specific scan.
     *
     * @param scanId The ID of the scan
     * @return List of schema info entities
     */
    List<SchemaInfo> findByScanId(UUID scanId);
    
    /**
     * Find a schema info by schema name.
     *
     * @param schemaName The name of the schema
     * @return Optional containing the schema info if found
     */
    Optional<SchemaInfo> findBySchemaName(String schemaName);
    
    /**
     * Delete a schema info.
     *
     * @param id The ID of the schema info to delete
     */
    void deleteById(UUID id);
    
    /**
     * Flush changes to the database.
     * Ensures all pending changes are immediately persisted.
     */
    void flush();
}