package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a group of correlated columns that together may form a quasi-identifier.
 * A quasi-identifier is a set of attributes that, while not directly identifying an individual,
 * can be combined to potentially re-identify data subjects.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "correlated_qi_groups")
public class CorrelatedQuasiIdentifierGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "group_name")
    private String groupName;
    
    @Column(name = "re_identification_risk_score")
    private double reIdentificationRiskScore;
    
    // Updated cascade strategy to only perform persist/merge operations, not remove operations
    @OneToMany(mappedBy = "group", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<QuasiIdentifierColumnMapping> columns = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "scan_id")
    private ScanMetadata scanMetadata;
    
    @Column(name = "distinct_combinations")
    private int distinctCombinations;
    
    @Column(name = "singleton_combinations")
    private int singletonCombinations;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "clustering_method")
    private String clusteringMethod;

    /**
     * Sets the clustering method used for this group
     * 
     * @param clusteringMethod the clustering method to set
     */
    public void setClusteringMethod(String clusteringMethod) {
        this.clusteringMethod = clusteringMethod;
    }

    /**
     * Gets the clustering method used for this group
     * 
     * @return the clustering method
     */
    public String getClusteringMethod() {
        return this.clusteringMethod;
    }

    /**
     * Adds a column to this quasi-identifier group with metrics
     * 
     * @param column the column to add
     * @param contributionScore the column's contribution score to the group's identification power
     * @param cardinality the number of distinct values in the column
     * @param distributionEntropy the entropy of the column's value distribution
     * @return the mapping object created
     */
    public QuasiIdentifierColumnMapping addColumn(
            ColumnInfo column, 
            double contributionScore, 
            int cardinality, 
            double distributionEntropy) {
            
        QuasiIdentifierColumnMapping mapping = new QuasiIdentifierColumnMapping();
        mapping.setColumn(column);
        mapping.setGroup(this);
        mapping.setContributionScore(contributionScore);
        mapping.setCardinality(cardinality);
        mapping.setDistributionEntropy(distributionEntropy);
        mapping.setCreatedAt(LocalDateTime.now());
        
        this.columns.add(mapping);
        return mapping;
    }
    
    /**
     * Calculates the estimated k-anonymity value for this group based on
     * the distinct combinations and singleton counts.
     * 
     * @return the estimated k-anonymity value
     */
    public double getKAnonymityValue() {
        if (distinctCombinations == 0) {
            return Double.MAX_VALUE; // No combinations means no risk
        }
        
        // If singletons available, use them for k-anonymity estimation
        if (singletonCombinations > 0) {
            return (double) distinctCombinations / singletonCombinations;
        }
        
        // Otherwise estimate based on available metrics
        // Use column cardinality as an estimate of population size
        Integer totalSamples = columns.stream()
                .map(mapping -> mapping.getCardinality())
                .findFirst()
                .orElse(0);
                
        if (totalSamples > 0) {
            return (double) totalSamples / distinctCombinations;
        }
        
        // Default fallback if no better estimate is available
        return 1.0;
    }

    /**
     * Gets the column names in this quasi-identifier group
     * 
     * @return list of column names
     */
    public List<String> getColumnNames() {
        return columns.stream()
                .map(mapping -> mapping.getColumn().getColumnName())
                .collect(Collectors.toList());
    }

    /**
     * Gets the group type based on clustering method
     * 
     * @return the group type
     */
    public String getGroupType() {
        return clusteringMethod != null ? clusteringMethod : "UNKNOWN";
    }

    /**
     * Gets the correlation score for this group
     * 
     * @return the correlation score
     */
    public double getCorrelationScore() {
        // Calculate average contribution score across all columns
        return columns.stream()
                .mapToDouble(QuasiIdentifierColumnMapping::getContributionScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Gets the privacy risk score for this group
     * 
     * @return the privacy risk score (same as re-identification risk)
     */
    public double getPrivacyRiskScore() {
        return reIdentificationRiskScore;
    }
}