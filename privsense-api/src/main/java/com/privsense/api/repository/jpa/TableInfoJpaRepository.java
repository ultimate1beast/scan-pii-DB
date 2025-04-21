package com.privsense.api.repository.jpa;

import com.privsense.core.model.TableInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for the TableInfo entity.
 */
@Repository
public interface TableInfoJpaRepository extends JpaRepository<TableInfo, UUID> {

    /**
     * Find all tables for a specific schema.
     *
     * @param schemaId The ID of the schema
     * @return List of table info entities
     */
    @Query("SELECT t FROM TableInfo t WHERE t.schema.id = :schemaId")
    List<TableInfo> findBySchemaId(@Param("schemaId") UUID schemaId);
    
    /**
     * Find a table by schema and table name.
     *
     * @param schemaId The ID of the schema
     * @param tableName The name of the table
     * @return Optional containing the table info if found
     */
    @Query("SELECT t FROM TableInfo t WHERE t.schema.id = :schemaId AND t.tableName = :tableName")
    Optional<TableInfo> findBySchemaIdAndTableName(
            @Param("schemaId") UUID schemaId, 
            @Param("tableName") String tableName);
}