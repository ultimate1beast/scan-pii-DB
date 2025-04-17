package com.privsense.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates all PII candidates found for a specific column after running the full detection pipeline.
 */
public class DetectionResult {

    private ColumnInfo columnInfo;
    private List<PiiCandidate> candidates;
    private String highestConfidencePiiType;
    private double highestConfidenceScore;
    private List<String> detectionMethods;

    public DetectionResult() {
        this.candidates = new ArrayList<>();
        this.detectionMethods = new ArrayList<>();
    }

    public DetectionResult(ColumnInfo columnInfo) {
        this();
        this.columnInfo = columnInfo;
    }

    public ColumnInfo getColumnInfo() {
        return columnInfo;
    }

    public void setColumnInfo(ColumnInfo columnInfo) {
        this.columnInfo = columnInfo;
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

    /**
     * @return true if any PII candidates were found, false otherwise
     */
    public boolean hasPii() {
        return candidates != null && !candidates.isEmpty();
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
                "columnInfo=" + columnInfo +
                ", highestConfidencePiiType='" + highestConfidencePiiType + '\'' +
                ", highestConfidenceScore=" + highestConfidenceScore +
                ", detectionMethods=" + detectionMethods +
                ", candidatesCount=" + (candidates != null ? candidates.size() : 0) +
                '}';
    }
}