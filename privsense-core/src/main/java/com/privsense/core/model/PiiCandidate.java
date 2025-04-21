package com.privsense.core.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a potential PII finding within a column.
 */
@Entity
@Table(name = "pii_candidates")
public class PiiCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "result_id")
    private DetectionResult detectionResult;

    @ManyToOne
    @JoinColumn(name = "column_id")
    private ColumnInfo columnInfo;

    @Column(name = "pii_type", nullable = false)
    private String piiType;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "detection_method", nullable = false)
    private String detectionMethod;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    public PiiCandidate() {
    }

    public PiiCandidate(ColumnInfo columnInfo, String piiType, double confidenceScore, String detectionMethod) {
        this.columnInfo = columnInfo;
        this.piiType = piiType;
        this.confidenceScore = confidenceScore;
        this.detectionMethod = detectionMethod;
    }

    public PiiCandidate(ColumnInfo columnInfo, String piiType, double confidenceScore, String detectionMethod, String evidence) {
        this(columnInfo, piiType, confidenceScore, detectionMethod);
        this.evidence = evidence;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DetectionResult getDetectionResult() {
        return detectionResult;
    }

    public void setDetectionResult(DetectionResult detectionResult) {
        this.detectionResult = detectionResult;
    }

    public ColumnInfo getColumnInfo() {
        return columnInfo;
    }

    public void setColumnInfo(ColumnInfo columnInfo) {
        this.columnInfo = columnInfo;
    }

    public String getPiiType() {
        return piiType;
    }

    public void setPiiType(String piiType) {
        this.piiType = piiType;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    @Override
    public String toString() {
        return "PiiCandidate{" +
                "id=" + id +
                ", piiType='" + piiType + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", detectionMethod='" + detectionMethod + '\'' +
                ", evidence='" + evidence + '\'' +
                '}';
    }
}