package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a saved scan configuration template
 * that can be reused for future scans.
 */
@Entity
@Table(name = "scan_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "connection_id")
    private UUID connectionId;

    @ElementCollection
    @CollectionTable(name = "scan_template_target_tables", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "target_table")
    private List<String> targetTables;

    // Sampling configuration
    @Column(name = "sampling_size")
    private Integer samplingSize;

    @Column(name = "sampling_method")
    private String samplingMethod;

    @Column(name = "entropy_calculation_enabled")
    private Boolean entropyCalculationEnabled;

    // Detection configuration
    @Column(name = "heuristic_threshold")
    private Double heuristicThreshold;

    @Column(name = "regex_threshold")
    private Double regexThreshold;

    @Column(name = "ner_threshold")
    private Double nerThreshold;

    @Column(name = "reporting_threshold")
    private Double reportingThreshold;

    @Column(name = "stop_pipeline_on_high_confidence")
    private Boolean stopPipelineOnHighConfidence;

    /**
     * Pre-persist hook to set timestamps before saving
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    /**
     * Pre-update hook to update the updatedAt timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}