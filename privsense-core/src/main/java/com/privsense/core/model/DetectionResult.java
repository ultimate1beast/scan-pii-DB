package com.privsense.core.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates all PII candidates found for a specific column after running the full detection pipeline.
 */
@Entity
@Table(name = "detection_results")
public class DetectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "scan_id")
    private ScanMetadata scanMetadata;

    @ManyToOne
    @JoinColumn(name = "column_id")
    private ColumnInfo columnInfo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_scan_id")
    private ComplianceReport report;
    
    @OneToMany(mappedBy = "detectionResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PiiCandidate> candidates;
    
    @Column(name = "highest_confidence_pii_type")
    private String highestConfidencePiiType;
    
    @Column(name = "highest_confidence_score")
    private double highestConfidenceScore;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "detection_methods", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "method_name")
    private List<String> detectionMethods;

    // Quasi-identifier related fields
    @Column(name = "is_quasi_identifier")
    private boolean isQuasiIdentifier;
    
    @Column(name = "quasi_identifier_risk_score")
    private double quasiIdentifierRiskScore;
    
    @Column(name = "quasi_identifier_type")
    private String quasiIdentifierType;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "correlated_columns", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "correlated_column")
    private List<String> correlatedColumns;

    public DetectionResult() {
        this.candidates = new ArrayList<>();
        this.detectionMethods = new ArrayList<>();
        this.correlatedColumns = new ArrayList<>();
        this.isQuasiIdentifier = false;
        this.quasiIdentifierRiskScore = 0.0;
    }

    public DetectionResult(ColumnInfo columnInfo) {
        this();
        this.columnInfo = columnInfo;
    }

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public ScanMetadata getScanMetadata() {
        return scanMetadata;
    }
    
    public void setScanMetadata(ScanMetadata scanMetadata) {
        this.scanMetadata = scanMetadata;
    }

    public ColumnInfo getColumnInfo() {
        return columnInfo;
    }

    public void setColumnInfo(ColumnInfo columnInfo) {
        this.columnInfo = columnInfo;
    }

    public ComplianceReport getReport() {
        return report;
    }
    
    public void setReport(ComplianceReport report) {
        this.report = report;
    }

    public List<PiiCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<PiiCandidate> candidates) {
        this.candidates = candidates;
        updateHighestConfidence();
        updateDetectionMethods();
    }

    public void addCandidate(PiiCandidate candidate) {
        if (this.candidates == null) {
            this.candidates = new ArrayList<>();
        }
        this.candidates.add(candidate);
        candidate.setDetectionResult(this);
        updateHighestConfidence();
        
        if (!detectionMethods.contains(candidate.getDetectionMethod())) {
            detectionMethods.add(candidate.getDetectionMethod());
        }
    }

    public String getHighestConfidencePiiType() {
        return highestConfidencePiiType;
    }

    public double getHighestConfidenceScore() {
        return highestConfidenceScore;
    }

    public List<String> getDetectionMethods() {
        return detectionMethods;
    }

    // Quasi-identifier getters and setters
    public boolean isQuasiIdentifier() {
        return isQuasiIdentifier;
    }

    public void setQuasiIdentifier(boolean quasiIdentifier) {
        isQuasiIdentifier = quasiIdentifier;
    }

    public double getQuasiIdentifierRiskScore() {
        return quasiIdentifierRiskScore;
    }

    public void setQuasiIdentifierRiskScore(double quasiIdentifierRiskScore) {
        this.quasiIdentifierRiskScore = quasiIdentifierRiskScore;
    }

    public String getQuasiIdentifierType() {
        return quasiIdentifierType;
    }

    public void setQuasiIdentifierType(String quasiIdentifierType) {
        this.quasiIdentifierType = quasiIdentifierType;
    }

    public List<String> getCorrelatedColumns() {
        return correlatedColumns;
    }

    public void setCorrelatedColumns(List<String> correlatedColumns) {
        this.correlatedColumns = correlatedColumns;
    }

    public void addCorrelatedColumn(String columnName) {
        if (this.correlatedColumns == null) {
            this.correlatedColumns = new ArrayList<>();
        }
        if (!this.correlatedColumns.contains(columnName)) {
            this.correlatedColumns.add(columnName);
        }
    }

    /**
     * @return true if any PII candidates were found, false otherwise
     */
    public boolean hasPii() {
        return candidates != null && !candidates.isEmpty();
    }

    /**
     * @return true if this column is a quasi-identifier or part of one, false otherwise
     */
    public boolean hasQuasiIdentifierRisk() {
        return isQuasiIdentifier || quasiIdentifierRiskScore > 0;
    }

    private void updateHighestConfidence() {
        if (candidates == null || candidates.isEmpty()) {
            highestConfidenceScore = 0.0;
            highestConfidencePiiType = null;
            return;
        }

        PiiCandidate highest = candidates.stream()
                .max((c1, c2) -> Double.compare(c1.getConfidenceScore(), c2.getConfidenceScore()))
                .orElse(null);

        if (highest != null) {
            highestConfidenceScore = highest.getConfidenceScore();
            highestConfidencePiiType = highest.getPiiType();
        }
    }

    private void updateDetectionMethods() {
        if (candidates == null || candidates.isEmpty()) {
            detectionMethods.clear();
            return;
        }

        detectionMethods.clear();
        candidates.stream()
                .map(PiiCandidate::getDetectionMethod)
                .distinct()
                .forEach(detectionMethods::add);
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
                "id=" + id +
                ", scanMetadata=" + scanMetadata +
                ", columnInfo=" + columnInfo +
                ", highestConfidencePiiType='" + highestConfidencePiiType + '\'' +
                ", highestConfidenceScore=" + highestConfidenceScore +
                ", detectionMethods=" + detectionMethods +
                ", isQuasiIdentifier=" + isQuasiIdentifier +
                ", quasiIdentifierRiskScore=" + quasiIdentifierRiskScore +
                ", quasiIdentifierType='" + quasiIdentifierType + '\'' +
                ", correlatedColumns=" + correlatedColumns +
                ", candidatesCount=" + (candidates != null ? candidates.size() : 0) +
                '}';
    }
}