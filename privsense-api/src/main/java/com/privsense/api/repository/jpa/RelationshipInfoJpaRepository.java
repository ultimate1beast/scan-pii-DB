package com.privsense.api.repository.jpa;

import com.privsense.core.model.RelationshipInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for the RelationshipInfo entity.
 */
@Repository
public interface RelationshipInfoJpaRepository extends JpaRepository<RelationshipInfo, UUID> {

    /**
     * Find all relationships for a specific source table.
     *
     * @param sourceTableId The ID of the source table
     * @return List of relationship info entities
     */
    List<RelationshipInfo> findBySourceTableId(UUID sourceTableId);
    
    /**
     * Find all relationships for a specific target table.
     *
     * @param targetTableId The ID of the target table
     * @return List of relationship info entities
     */
    List<RelationshipInfo> findByTargetTableId(UUID targetTableId);
    
    /**
     * Find a relationship by its constraint name.
     *
     * @param constraintName The name of the constraint
     * @return Optional containing the relationship info if found
     */
    Optional<RelationshipInfo> findByConstraintName(String constraintName);
}