package com.privsense.core.repository;

import com.privsense.core.model.ScanTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for scan template operations.
 */
public interface ScanTemplateRepository {

    /**
     * Saves a scan template.
     *
     * @param template The scan template to save
     * @return The saved template with its ID
     */
    ScanTemplate save(ScanTemplate template);

    /**
     * Checks if a template exists.
     *
     * @param id The template ID
     * @return true if the template exists, false otherwise
     */
    boolean existsById(UUID id);

    /**
     * Gets a template by its ID.
     *
     * @param id The template ID
     * @return An Optional containing the template if found
     */
    Optional<ScanTemplate> findById(UUID id);

    /**
     * Gets all templates.
     *
     * @return A list of all scan templates
     */
    List<ScanTemplate> findAll();

    /**
     * Gets all templates for a specific connection.
     *
     * @param connectionId The connection ID
     * @return A list of templates for the connection
     */
    List<ScanTemplate> findByConnectionId(UUID connectionId);

    /**
     * Deletes a template by its ID.
     *
     * @param id The template ID
     * @return true if the template was deleted, false if it didn't exist
     */
    boolean deleteById(UUID id);

    /**
     * Checks if a template with the given name exists.
     *
     * @param name The template name
     * @return true if a template with that name exists, false otherwise
     */
    boolean existsByName(String name);
}