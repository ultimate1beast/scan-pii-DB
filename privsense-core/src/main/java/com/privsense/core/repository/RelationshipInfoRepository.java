package com.privsense.core.repository;

import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.model.TableInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing RelationshipInfo entities.
 */
public interface RelationshipInfoRepository {
    
    /**
     * Save a relationship info entity.
     *
     * @param relationshipInfo The relationship info to save
     * @return The saved entity
     */
    RelationshipInfo save(RelationshipInfo relationshipInfo);
    
    /**
     * Save a collection of relationship info entities.
     *
     * @param relationshipInfos The collection of relationship infos to save
     * @return The saved entities
     */
    Iterable<RelationshipInfo> saveAll(Collection<RelationshipInfo> relationshipInfos);
    
    /**
     * Find a relationship info by its ID.
     *
     * @param id The ID of the relationship info
     * @return Optional containing the relationship info if found
     */
    Optional<RelationshipInfo> findById(UUID id);
    
    /**
     * Find all relationship info entities for a specific source table.
     *
     * @param sourceTableId The ID of the source table
     * @return List of relationship info entities
     */
    List<RelationshipInfo> findBySourceTableId(UUID sourceTableId);
    
    /**
     * Find all relationship info entities for a specific target table.
     *
     * @param targetTableId The ID of the target table
     * @return List of relationship info entities
     */
    List<RelationshipInfo> findByTargetTableId(UUID targetTableId);
    
    /**
     * Find a relationship info by its constraint name.
     *
     * @param constraintName The name of the constraint
     * @return Optional containing the relationship info if found
     */
    Optional<RelationshipInfo> findByConstraintName(String constraintName);
    
    /**
     * Delete a relationship info.
     *
     * @param id The ID of the relationship info to delete
     */
    void deleteById(UUID id);
}