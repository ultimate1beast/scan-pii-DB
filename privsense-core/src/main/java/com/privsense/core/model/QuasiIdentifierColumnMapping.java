package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapping entity that connects a database column to a quasi-identifier group
 * with additional metrics about the column's role in the group.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "quasi_identifier_columns")
public class QuasiIdentifierColumnMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    // Modified cascade settings to ensure proper persistence of referenced entities
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "column_id")
    private ColumnInfo column;
    
    @ManyToOne
    @JoinColumn(name = "group_id")
    private CorrelatedQuasiIdentifierGroup group;
    
    /**
     * The contribution score indicates how much this column contributes to
     * the identifying power of the quasi-identifier group.
     * Higher values indicate stronger contribution.
     */
    @Column(name = "contribution_score")
    private double contributionScore;
    
    /**
     * The number of distinct values found in this column
     */
    @Column(name = "cardinality")
    private int cardinality;
    
    /**
     * The Shannon entropy of the column's value distribution
     */
    @Column(name = "distribution_entropy")
    private double distributionEntropy;
    
    /**
     * When this mapping was created
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}