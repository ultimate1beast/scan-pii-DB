package com.privsense.api.repository.jpa;

import com.privsense.core.model.ColumnInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for the ColumnInfo entity.
 */
@Repository
public interface ColumnInfoJpaRepository extends JpaRepository<ColumnInfo, UUID> {

    /**
     * Find all columns for a specific table.
     *
     * @param tableId The ID of the table
     * @return List of column info entities
     */
    List<ColumnInfo> findByTableId(UUID tableId);
    
    /**
     * Find a column by table and column name.
     *
     * @param tableName The name of the table
     * @param columnName The name of the column
     * @return Optional containing the column info if found
     */
    @Query("SELECT c FROM ColumnInfo c JOIN c.table t WHERE t.tableName = :tableName AND c.columnName = :columnName")
    Optional<ColumnInfo> findByTableNameAndColumnName(
            @Param("tableName") String tableName, 
            @Param("columnName") String columnName);
}