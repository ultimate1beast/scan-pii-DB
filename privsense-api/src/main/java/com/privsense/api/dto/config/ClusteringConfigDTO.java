package com.privsense.api.dto.config;

import com.privsense.api.dto.base.BaseConfigDTO;
import com.privsense.api.validation.Threshold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for ML-based clustering of quasi-identifiers.
 * Controls how the system identifies clusters of columns that together
 * may pose a privacy risk.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClusteringConfigDTO extends BaseConfigDTO {
    
    /**
     * Whether ML-based clustering is enabled.
     */
    @NotNull
    @Builder.Default
    private Boolean enabled = true;
    
    /**
     * The clustering algorithm to use.
     * Supported values: DBSCAN, HIERARCHICAL, KMEANS
     */
    @NotNull
    @Builder.Default
    private String algorithm = "DBSCAN";
    
    /**
     * The distance threshold for clustering algorithms.
     * Lower values create tighter, more precise clusters.
     */
    @Threshold
    @Builder.Default
    private Double distanceThreshold = 0.5;
    
    /**
     * Minimum number of columns needed to form a cluster.
     */
    @Min(value = 2, message = "Minimum cluster size must be at least 2")
    @Max(value = 20, message = "Minimum cluster size must be at most 20")
    @Builder.Default
    private Integer minClusterSize = 2;
    
    /**
     * Maximum number of clusters to detect (primarily for K-means).
     */
    @Min(value = 1, message = "Maximum clusters must be at least 1")
    @Max(value = 50, message = "Maximum clusters must be at most 50")
    @Builder.Default
    private Integer maxClusters = 10;
    
    /**
     * The feature selection method for column comparison.
     * Supported values: ALL, STATISTICAL, METADATA
     */
    @Builder.Default
    private String featureSelection = "ALL";
    
    /**
     * Whether to incorporate actual column values in the clustering analysis.
     * When true, column values are used to compute statistical features.
     * When false, only column metadata (type, name patterns) is used.
     */
    @Builder.Default
    private Boolean useColumnValues = true;
}