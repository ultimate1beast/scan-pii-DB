package com.privsense.api.repository.jpa;

import com.privsense.core.model.ScanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for the ScanTemplate entity.
 */
@Repository
public interface ScanTemplateJpaRepository extends JpaRepository<ScanTemplate, UUID> {
    
    /**
     * Finds templates for a specific connection.
     *
     * @param connectionId The connection ID
     * @return A list of templates for the connection
     */
    List<ScanTemplate> findByConnectionId(UUID connectionId);
    
    /**
     * Checks if a template with the given name exists.
     *
     * @param name The template name
     * @return true if a template with that name exists, false otherwise
     */
    boolean existsByName(String name);
}