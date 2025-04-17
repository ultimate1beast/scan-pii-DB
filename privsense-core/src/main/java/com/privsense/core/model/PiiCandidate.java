package com.privsense.core.model;

/**
 * Represents a potential PII finding within a column.
 */
public class PiiCandidate {

    private ColumnInfo columnInfo;
    private String piiType;
    private double confidenceScore;
    private String detectionMethod;
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
                "columnInfo=" + columnInfo +
                ", piiType='" + piiType + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", detectionMethod='" + detectionMethod + '\'' +
                ", evidence='" + evidence + '\'' +
                '}';
    }
}