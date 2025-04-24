package com.privsense.core.repository;

import com.privsense.core.model.ColumnInfo;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing ColumnInfo entities.
 */
public interface ColumnInfoRepository {
    
    /**
     * Save a column info entity.
     *
     * @param columnInfo The column info to save
     * @return The saved entity
     */
    ColumnInfo save(ColumnInfo columnInfo);
    
    /**
     * Save a collection of column info entities.
     *
     * @param columnInfos The collection of column infos to save
     * @return The saved entities
     */
    Iterable<ColumnInfo> saveAll(Collection<ColumnInfo> columnInfos);
    
    /**
     * Find a column info by its ID.
     *
     * @param id The ID of the column info
     * @return Optional containing the column info if found
     */
    Optional<ColumnInfo> findById(UUID id);
    
    /**
     * Find all column info entities for a specific table.
     *
     * @param tableId The ID of the table
     * @return List of column info entities
     */
    List<ColumnInfo> findByTableId(UUID tableId);
    
    /**
     * Find a column info by table and column name.
     *
     * @param tableName The name of the table
     * @param columnName The name of the column
     * @return Optional containing the column info if found
     */
    Optional<ColumnInfo> findByTableNameAndColumnName(String tableName, String columnName);
    
    /**
     * Delete a column info.
     *
     * @param id The ID of the column info to delete
     */
    void deleteById(UUID id);
}