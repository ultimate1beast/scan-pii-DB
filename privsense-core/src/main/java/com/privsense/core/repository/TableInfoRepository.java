package com.privsense.core.repository;

import com.privsense.core.model.TableInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing TableInfo entities.
 */
public interface TableInfoRepository {
    
    /**
     * Save a table info entity.
     *
     * @param tableInfo The table info to save
     * @return The saved entity
     */
    TableInfo save(TableInfo tableInfo);
    
    /**
     * Save a collection of table info entities.
     *
     * @param tableInfos The collection of table infos to save
     * @return The saved entities
     */
    Iterable<TableInfo> saveAll(Collection<TableInfo> tableInfos);
    
    /**
     * Find a table info by its ID.
     *
     * @param id The ID of the table info
     * @return Optional containing the table info if found
     */
    Optional<TableInfo> findById(UUID id);
    
    /**
     * Find all table info entities for a specific schema.
     *
     * @param schemaId The ID of the schema
     * @return List of table info entities
     */
    List<TableInfo> findBySchemaId(UUID schemaId);
    
    /**
     * Find a table info by schema and table name.
     *
     * @param schemaId The ID of the schema
     * @param tableName The name of the table
     * @return Optional containing the table info if found
     */
    Optional<TableInfo> findBySchemaIdAndTableName(UUID schemaId, String tableName);
    
    /**
     * Delete a table info.
     *
     * @param id The ID of the table info to delete
     */
    void deleteById(UUID id);
}