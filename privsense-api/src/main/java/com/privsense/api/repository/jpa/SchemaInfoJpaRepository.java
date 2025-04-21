package com.privsense.api.repository.jpa;

import com.privsense.core.model.SchemaInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository interface for SchemaInfo entities.
 */
@Repository
public interface SchemaInfoJpaRepository extends JpaRepository<SchemaInfo, UUID> {
    
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
}