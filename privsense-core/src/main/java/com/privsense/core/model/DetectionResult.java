package com.privsense.core.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * Aggregates all PII candidates found for a specific column after running the full detection pipeline.
 */
@Entity
@Table(name = "detection_results")
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String highestConfidencePiiType = "UNKNOWN";

    @Column(name = "highest_confidence_score")
    private double highestConfidenceScore = 0.0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "detection_methods", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "method_name")
    private List<String> detectionMethods;

    @Column(name = "is_pii")
    private boolean isPii = false; // Standard isPii based on general threshold

    @Column(name = "is_quasi_identifier")
    private boolean isQuasiIdentifier;

    @Column(name = "quasi_identifier_risk_score")
    private Double quasiIdentifierRiskScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "correlated_columns", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "column_name")
    private List<String> correlatedColumns;

    @Column(name = "clustering_method")
    private String clusteringMethod;

    private boolean sensitiveData = false;
    private Map<String, Object> attributes = new HashMap<>();
    private String status = "SUCCESS";
    private boolean success = true;
    private String errorMessage;

    public DetectionResult() {
        this.candidates = new ArrayList<>();
        this.detectionMethods = new ArrayList<>();
        this.correlatedColumns = new ArrayList<>();
        this.isQuasiIdentifier = false;
        this.quasiIdentifierRiskScore = 0.0;
        this.clusteringMethod = null;
        this.isPii = false;
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

    public void setHighestConfidencePiiType(String highestConfidencePiiType) {
        this.highestConfidencePiiType = highestConfidencePiiType;
    }

    public double getHighestConfidenceScore() {
        return highestConfidenceScore;
    }

    public void setHighestConfidenceScore(double highestConfidenceScore) {
        this.highestConfidenceScore = highestConfidenceScore;
    }

    @JsonIgnore
    public boolean hasPii() {
        // Updated to check detectionMethods and confidenceScore against thresholds
        return isPii() || sensitiveData || isQuasiIdentifier;
    }

    @JsonProperty("isPii")
    public boolean isPii() {
        // If both the attribute and field exist but disagree, attribute takes precedence
        if (attributes.containsKey("isPii")) {
            boolean attributeValue = (boolean) attributes.get("isPii");
            // Keep the field in sync with the attribute
            if (attributeValue != isPii) {
                isPii = attributeValue;
            }
            return attributeValue;
        }
        return isPii;
    }

    public void setPii(boolean isPii) {
        this.isPii = isPii;
        // Keep the attribute in sync with the field
        attributes.put("isPii", isPii);
    }

    public boolean isSensitiveData() {
        return sensitiveData;
    }

    public void setSensitiveData(boolean sensitiveData) {
        this.sensitiveData = sensitiveData;
    }

    public boolean isQuasiIdentifier() {
        return isQuasiIdentifier;
    }

    public void setQuasiIdentifier(boolean isQuasiIdentifier) {
        this.isQuasiIdentifier = isQuasiIdentifier;
    }

    public Double getQuasiIdentifierRiskScore() {
        return quasiIdentifierRiskScore;
    }

    public void setQuasiIdentifierRiskScore(Double quasiIdentifierRiskScore) {
        this.quasiIdentifierRiskScore = quasiIdentifierRiskScore;
    }

    public List<String> getCorrelatedColumns() {
        return correlatedColumns;
    }

    public void setCorrelatedColumns(List<String> correlatedColumns) {
        this.correlatedColumns = correlatedColumns;
    }

    public String getClusteringMethod() {
        return clusteringMethod;
    }

    public void setClusteringMethod(String clusteringMethod) {
        this.clusteringMethod = clusteringMethod;
    }

    @JsonProperty("piiType")
    public String getPiiType() {
        return highestConfidencePiiType;
    }

    @JsonProperty("confidenceScore")
    public double getConfidenceScore() {
        return highestConfidenceScore;
    }

    @JsonProperty("tableName")
    public String getTableName() {
        if (columnInfo != null && columnInfo.getTable() != null) {
            return columnInfo.getTable().getTableName();
        }
        return null;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnInfo != null ? columnInfo.getColumnName() : null;
    }

    @JsonProperty("dataType")
    public String getDataType() {
        if (columnInfo != null) {
            // Use database type name if available
            if (columnInfo.getDatabaseTypeName() != null && !columnInfo.getDatabaseTypeName().isEmpty()) {
                return columnInfo.getDatabaseTypeName();
            }
            
            // Otherwise convert JDBC type to string representation
            int jdbcType = columnInfo.getJdbcType();
            // Convert JDBC type code to string representation
            switch (jdbcType) {
                case java.sql.Types.VARCHAR:
                case java.sql.Types.CHAR:
                case java.sql.Types.LONGVARCHAR:
                    return "VARCHAR";
                case java.sql.Types.NUMERIC:
                case java.sql.Types.DECIMAL:
                    return "DECIMAL";
                case java.sql.Types.INTEGER:
                    return "INT";
                case java.sql.Types.BIGINT:
                    return "BIGINT";
                case java.sql.Types.SMALLINT:
                    return "SMALLINT";
                case java.sql.Types.FLOAT:
                case java.sql.Types.DOUBLE:
                    return "FLOAT";
                case java.sql.Types.DATE:
                    return "DATE";
                case java.sql.Types.TIME:
                    return "TIME";
                case java.sql.Types.TIMESTAMP:
                    return "TIMESTAMP";
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return "BINARY";
                case java.sql.Types.BLOB:
                    return "BLOB";
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    return "TEXT";
                case java.sql.Types.BOOLEAN:
                    return "BOOLEAN";
                default:
                    return "UNKNOWN";
            }
        }
        return null;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            this.status = "ERROR";
            this.success = false;
        }
    }

    private void updateHighestConfidence() {
        if (candidates == null || candidates.isEmpty()) {
            highestConfidenceScore = 0.0;
            highestConfidencePiiType = null;
            isPii = false;
            return;
        }

        PiiCandidate highest = candidates.stream()
                .max((c1, c2) -> Double.compare(c1.getConfidenceScore(), c2.getConfidenceScore()))
                .orElse(null);

        if (highest != null) {
            highestConfidenceScore = highest.getConfidenceScore();
            highestConfidencePiiType = highest.getPiiType();
            
            // Check detection method and set isPii flag based on appropriate threshold
            String detectionMethod = highest.getDetectionMethod();
            double thresholdForMethod = 0.5; // Default threshold
            
            // This is just a simple implementation - actually should be getting from config
            if ("REGEX".equals(detectionMethod)) {
                thresholdForMethod = 0.8; // REGEX threshold
            } else if ("HEURISTIC".equals(detectionMethod)) {
                thresholdForMethod = 0.7; // HEURISTIC threshold
            } else if ("NER".equals(detectionMethod)) {
                thresholdForMethod = 0.6; // NER threshold
            }
            
            // Set the PII flag if the confidence score meets or exceeds the threshold
            boolean shouldBePii = highest.getConfidenceScore() >= thresholdForMethod;
            setPii(shouldBePii);
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
                ", isPii=" + isPii +
                ", isQuasiIdentifier=" + isQuasiIdentifier +
                ", quasiIdentifierRiskScore=" + quasiIdentifierRiskScore +
                ", correlatedColumns=" + correlatedColumns +
                ", clusteringMethod='" + clusteringMethod + '\'' +
                ", candidatesCount=" + (candidates != null ? candidates.size() : 0) +
                '}';
    }
}