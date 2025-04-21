package com.privsense.api.repository.jpa;

import com.privsense.core.model.ScanMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ScanMetadata entities.
 */
@Repository
public interface ScanMetadataJpaRepository extends JpaRepository<ScanMetadata, UUID> {
    
    /**
     * Find all scans for a specific connection.
     *
     * @param connectionId The connection ID
     * @return List of scan metadata entities
     */
    List<ScanMetadata> findByConnectionId(UUID connectionId);
    
    /**
     * Find all scans with a specific status.
     *
     * @param status The scan status
     * @return List of scan metadata entities
     */
    List<ScanMetadata> findByStatus(ScanMetadata.ScanStatus status);
    
    /**
     * Find recent scans performed after a specific time.
     *
     * @param startTime The start time to search from
     * @return List of scan metadata entities
     */
    @Query("SELECT s FROM ScanMetadata s WHERE s.startTime >= :startTime")
    List<ScanMetadata> findRecentScans(@Param("startTime") Instant startTime);
}