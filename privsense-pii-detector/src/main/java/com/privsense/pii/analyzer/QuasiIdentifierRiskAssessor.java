package com.privsense.pii.analyzer;

import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assesses re-identification risk of quasi-identifiers found in database columns.
 * 
 * This class calculates k-anonymity, l-diversity, and other privacy metrics
 * to help quantify the risk associated with quasi-identifiers.
 */
@Component
public class QuasiIdentifierRiskAssessor {
    
    private static final Logger logger = LoggerFactory.getLogger(QuasiIdentifierRiskAssessor.class);
    
    private final PrivSenseConfigProperties configProperties;
    
    /**
     * Constructor with config properties injection
     */
    @Autowired
    public QuasiIdentifierRiskAssessor(PrivSenseConfigProperties configProperties) {
        this.configProperties = configProperties;
    }
    
    /**
     * Assesses the privacy risk posed by quasi-identifiers in the detection results.
     * 
     * @param results List of detection results to analyze
     * @param columnSamples Map of columns to sample data
     * @return A RiskAssessmentReport detailing privacy risks
     */
    public RiskAssessmentReport assessRisk(List<DetectionResult> results, Map<ColumnInfo, SampleData> columnSamples) {
        logger.debug("Starting risk assessment for {} columns", results.size());
        
        // Filter for results containing quasi-identifier candidates
        List<DetectionResult> quasiIdResults = results.stream()
                .filter(this::hasQuasiIdentifierCandidate)
                .collect(Collectors.toList());
        
        if (quasiIdResults.isEmpty()) {
            logger.info("No quasi-identifiers found, risk assessment skipped");
            return new RiskAssessmentReport(RiskLevel.LOW, Collections.emptyMap(), Collections.emptyMap());
        }
        
        // Group quasi-identifiers by table
        Map<String, List<DetectionResult>> resultsByTable = quasiIdResults.stream()
                .collect(Collectors.groupingBy(r -> r.getColumnInfo().getTable().getTableName()));
        
        // Determine overall risk level
        RiskLevel overallRisk = RiskLevel.LOW;
        Map<String, RiskLevel> tableRisks = new HashMap<>();
        Map<String, Map<String, ColumnRiskInfo>> columnRisks = new HashMap<>();
        
        for (Map.Entry<String, List<DetectionResult>> entry : resultsByTable.entrySet()) {
            String tableName = entry.getKey();
            List<DetectionResult> tableResults = entry.getValue();
            
            // Calculate table-level risk
            RiskLevel tableRisk = assessTableRisk(tableName, tableResults, columnSamples);
            tableRisks.put(tableName, tableRisk);
            
            // Calculate column-level risks
            Map<String, ColumnRiskInfo> tableColumnRisks = new HashMap<>();
            for (DetectionResult result : tableResults) {
                ColumnInfo columnInfo = result.getColumnInfo();
                ColumnRiskInfo riskInfo = assessColumnRisk(result, columnSamples.get(columnInfo));
                tableColumnRisks.put(columnInfo.getColumnName(), riskInfo);
            }
            columnRisks.put(tableName, tableColumnRisks);
            
            // Update overall risk (use the highest risk level found)
            if (tableRisk.compareTo(overallRisk) > 0) {
                overallRisk = tableRisk;
            }
        }
        
        RiskAssessmentReport report = new RiskAssessmentReport(overallRisk, tableRisks, columnRisks);
        logger.info("Risk assessment completed. Overall risk: {}", overallRisk);
        return report;
    }
    
    /**
     * Assesses the risk level for a specific table based on its quasi-identifiers
     */
    private RiskLevel assessTableRisk(
            String tableName, 
            List<DetectionResult> results, 
            Map<ColumnInfo, SampleData> columnSamples) {
        
        // Extract columns that are quasi-identifiers
        List<ColumnInfo> quasiIdColumns = results.stream()
                .map(DetectionResult::getColumnInfo)
                .collect(Collectors.toList());
        
        if (quasiIdColumns.isEmpty()) {
            return RiskLevel.LOW;
        }
        
        // Get column samples for k-anonymity calculation
        List<SampleData> sampleDataList = new ArrayList<>();
        for (ColumnInfo column : quasiIdColumns) {
            SampleData samples = columnSamples.get(column);
            if (samples != null) {
                sampleDataList.add(samples);
            }
        }
        
        // Calculate k-anonymity
        int kAnonymity = calculateKAnonymity(sampleDataList);
        
        // Calculate risk level based on k-anonymity
        RiskLevel risk;
        if (kAnonymity <= 1) {
            risk = RiskLevel.CRITICAL; // Unique identification possible
        } else if (kAnonymity <= 5) {
            risk = RiskLevel.HIGH;   // Very small anonymity set
        } else if (kAnonymity <= 15) {
            risk = RiskLevel.MEDIUM; // Moderate anonymity set
        } else {
            risk = RiskLevel.LOW;    // Large anonymity set
        }
        
        logger.debug("Table {} has k-anonymity of {} (Risk: {})", tableName, kAnonymity, risk);
        return risk;
    }
    
    /**
     * Assesses the risk level for a specific column
     */
    private ColumnRiskInfo assessColumnRisk(DetectionResult result, SampleData sampleData) {
        ColumnInfo columnInfo = result.getColumnInfo();
        
        // Find the quasi-identifier candidate
        Optional<PiiCandidate> quasiCandidate = result.getCandidates().stream()
                .filter(c -> "QUASI_IDENTIFIER".equals(c.getDetectionMethod()))
                .findFirst();
        
        if (!quasiCandidate.isPresent() || sampleData == null) {
            return new ColumnRiskInfo(RiskLevel.LOW, 0, 0, Collections.emptyList());
        }
        
        // Calculate metrics
        int distinctValueCount = calculateDistinctValueCount(sampleData);
        int totalSamples = sampleData.getSamples().size();
        double confidenceScore = quasiCandidate.get().getConfidenceScore();
        
        // Calculate relative distinctness
        double distinctRatio = totalSamples > 0 ? (double) distinctValueCount / totalSamples : 0;
        
        // Calculate risk level based on distinctness and confidence
        RiskLevel risk;
        if (distinctRatio >= 0.9 && confidenceScore >= 0.8) {
            risk = RiskLevel.CRITICAL;  // Nearly unique values with high confidence
        } else if (distinctRatio >= 0.7 && confidenceScore >= 0.7) {
            risk = RiskLevel.HIGH;      // High distinctness with moderate confidence
        } else if ((distinctRatio >= 0.5 && confidenceScore >= 0.6) ||
                  (distinctRatio >= 0.3 && confidenceScore >= 0.8)) {
            risk = RiskLevel.MEDIUM;    // Either moderate distinctness or high confidence
        } else {
            risk = RiskLevel.LOW;       // Low distinctness and low confidence
        }
        
        // Find frequently correlated columns based on evidence
        List<String> correlatedColumns = new ArrayList<>();
        for (PiiCandidate candidate : result.getCandidates()) {
            if (candidate.getEvidence() != null && 
                    candidate.getEvidence().contains("Correlated with column:")) {
                // Extract correlated column name from evidence text
                String[] lines = candidate.getEvidence().split("\n");
                for (String line : lines) {
                    if (line.contains("Correlated with column:")) {
                        String columnName = line.substring(line.indexOf(":") + 1).trim();
                        // Extract just the column name from the evidence
                        if (columnName.contains(".")) {
                            columnName = columnName.substring(columnName.indexOf(".") + 1);
                        }
                        if (columnName.contains(" ")) {
                            columnName = columnName.substring(0, columnName.indexOf(" "));
                        }
                        correlatedColumns.add(columnName);
                    }
                }
            }
        }
        
        logger.debug("Column {}.{} risk assessment: {} (Distinct values: {}/{})", 
                columnInfo.getTable().getTableName(), columnInfo.getColumnName(),
                risk, distinctValueCount, totalSamples);
                
        return new ColumnRiskInfo(risk, distinctValueCount, totalSamples, correlatedColumns);
    }
    
    /**
     * Calculate k-anonymity value based on column samples
     * K-anonymity is the smallest size of any equivalence class (group of records
     * with the same values for quasi-identifiers)
     */
    private int calculateKAnonymity(List<SampleData> sampleDataList) {
        if (sampleDataList.isEmpty()) {
            return Integer.MAX_VALUE; // No risk if no data
        }
        
        int rowCount = sampleDataList.get(0).getSamples().size();
        
        // Create a map of row index to its equivalence class signature
        Map<Integer, StringBuilder> rowSignatures = new HashMap<>();
        
        // Initialize signatures
        for (int i = 0; i < rowCount; i++) {
            rowSignatures.put(i, new StringBuilder());
        }
        
        // Build signatures by combining all quasi-identifier values
        for (SampleData columnSamples : sampleDataList) {
            List<Object> samples = columnSamples.getSamples();
            for (int i = 0; i < Math.min(rowCount, samples.size()); i++) {
                Object value = samples.get(i);
                rowSignatures.get(i).append(value != null ? value.toString() : "NULL").append("|");
            }
        }
        
        // Count occurrences of each signature to find equivalence classes
        Map<String, Integer> signatureCounts = new HashMap<>();
        for (StringBuilder sig : rowSignatures.values()) {
            String signature = sig.toString();
            signatureCounts.put(signature, signatureCounts.getOrDefault(signature, 0) + 1);
        }
        
        // Find the smallest equivalence class size (k)
        int k = signatureCounts.isEmpty() ? Integer.MAX_VALUE : 
                Collections.min(signatureCounts.values());
        
        // Handle empty or singleton case
        if (k == Integer.MAX_VALUE) {
            k = rowCount; // All records are distinct
        }
        
        return k;
    }
    
    /**
     * Calculates the number of distinct values in the sample data
     */
    private int calculateDistinctValueCount(SampleData sampleData) {
        if (sampleData == null || sampleData.getSamples() == null) {
            return 0;
        }
        
        return (int) sampleData.getSamples().stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .count();
    }
    
    /**
     * Checks if a result contains quasi-identifier candidates
     */
    private boolean hasQuasiIdentifierCandidate(DetectionResult result) {
        return result.getCandidates().stream()
                .anyMatch(candidate -> "QUASI_IDENTIFIER".equals(candidate.getDetectionMethod()) ||
                        candidate.getPiiType().startsWith("QUASI_ID"));
    }
    
    /**
     * Risk levels for privacy assessment
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL;
        
        public String getDescription() {
            switch (this) {
                case LOW:
                    return "Low risk - Minimal re-identification possibility";
                case MEDIUM:
                    return "Medium risk - Some re-identification possibility under certain conditions";
                case HIGH:
                    return "High risk - Significant re-identification possibility";
                case CRITICAL:
                    return "Critical risk - High probability of individual re-identification";
                default:
                    return "Unknown risk level";
            }
        }
    }
    
    /**
     * Risk information for a specific column
     */
    public static class ColumnRiskInfo {
        private final RiskLevel riskLevel;
        private final int distinctValueCount;
        private final int totalSamples;
        private final List<String> correlatedColumns;
        
        public ColumnRiskInfo(
                RiskLevel riskLevel, 
                int distinctValueCount, 
                int totalSamples, 
                List<String> correlatedColumns) {
            this.riskLevel = riskLevel;
            this.distinctValueCount = distinctValueCount;
            this.totalSamples = totalSamples;
            this.correlatedColumns = correlatedColumns;
        }
        
        public RiskLevel getRiskLevel() {
            return riskLevel;
        }
        
        public int getDistinctValueCount() {
            return distinctValueCount;
        }
        
        public int getTotalSamples() {
            return totalSamples;
        }
        
        public double getDistinctRatio() {
            return totalSamples > 0 ? (double) distinctValueCount / totalSamples : 0;
        }
        
        public List<String> getCorrelatedColumns() {
            return correlatedColumns;
        }
        
        @Override
        public String toString() {
            return String.format("Risk: %s, Distinct values: %d/%d (%.1f%%)", 
                    riskLevel, distinctValueCount, totalSamples, getDistinctRatio() * 100);
        }
    }
    
    /**
     * Report containing risk assessment results
     */
    public static class RiskAssessmentReport {
        private final RiskLevel overallRisk;
        private final Map<String, RiskLevel> tableRisks;
        private final Map<String, Map<String, ColumnRiskInfo>> columnRisks;
        
        public RiskAssessmentReport(
                RiskLevel overallRisk, 
                Map<String, RiskLevel> tableRisks, 
                Map<String, Map<String, ColumnRiskInfo>> columnRisks) {
            this.overallRisk = overallRisk;
            this.tableRisks = tableRisks;
            this.columnRisks = columnRisks;
        }
        
        public RiskLevel getOverallRisk() {
            return overallRisk;
        }
        
        public Map<String, RiskLevel> getTableRisks() {
            return tableRisks;
        }
        
        public Map<String, Map<String, ColumnRiskInfo>> getColumnRisks() {
            return columnRisks;
        }
        
        /**
         * Gets highest risk columns across all tables
         */
        public List<Map.Entry<String, ColumnRiskInfo>> getHighRiskColumns() {
            List<Map.Entry<String, ColumnRiskInfo>> highRiskColumns = new ArrayList<>();
            
            for (Map.Entry<String, Map<String, ColumnRiskInfo>> tableEntry : columnRisks.entrySet()) {
                String tableName = tableEntry.getKey();
                for (Map.Entry<String, ColumnRiskInfo> columnEntry : tableEntry.getValue().entrySet()) {
                    if (columnEntry.getValue().getRiskLevel() == RiskLevel.HIGH || 
                            columnEntry.getValue().getRiskLevel() == RiskLevel.CRITICAL) {
                        
                        // Using a custom entry implementation to include the table name
                        Map.Entry<String, ColumnRiskInfo> entry = new AbstractMap.SimpleEntry<>(
                                tableName + "." + columnEntry.getKey(), columnEntry.getValue());
                        highRiskColumns.add(entry);
                    }
                }
            }
            
            return highRiskColumns;
        }
        
        /**
         * Gets mitigation recommendations based on findings
         */
        public List<String> getMitigationRecommendations() {
            List<String> recommendations = new ArrayList<>();
            
            // General recommendations based on overall risk
            switch (overallRisk) {
                case CRITICAL:
                    recommendations.add("URGENT: Implement data anonymization techniques such as generalization or suppression");
                    recommendations.add("Consider implementing differential privacy or synthetic data generation");
                    recommendations.add("Review and potentially restrict access to high-risk tables");
                    break;
                    
                case HIGH:
                    recommendations.add("Implement k-anonymity protections with k ≥ 5");
                    recommendations.add("Consider column-level encryption for high-risk columns");
                    recommendations.add("Review access controls for tables with high risk scores");
                    break;
                    
                case MEDIUM:
                    recommendations.add("Implement k-anonymity protections with k ≥ 3");
                    recommendations.add("Consider data masking for medium-risk columns");
                    break;
                    
                case LOW:
                    recommendations.add("Monitor quasi-identifiers periodically as data evolves");
                    recommendations.add("Document current low-risk status for compliance purposes");
                    break;
            }
            
            // Add specific recommendations for correlated columns
            boolean hasCorrelatedColumns = false;
            for (Map<String, ColumnRiskInfo> tableColumns : columnRisks.values()) {
                for (ColumnRiskInfo columnRisk : tableColumns.values()) {
                    if (!columnRisk.getCorrelatedColumns().isEmpty()) {
                        hasCorrelatedColumns = true;
                        break;
                    }
                }
            }
            
            if (hasCorrelatedColumns) {
                recommendations.add("Consider implementing l-diversity to protect against attribute disclosure in correlated columns");
                recommendations.add("Review and potentially restrict join operations between tables with correlated quasi-identifiers");
            }
            
            return recommendations;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Risk Assessment Report\n");
            sb.append("---------------------\n");
            sb.append("Overall Risk: ").append(overallRisk).append(" - ").append(overallRisk.getDescription()).append("\n\n");
            
            sb.append("Table Risk Levels:\n");
            for (Map.Entry<String, RiskLevel> entry : tableRisks.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            sb.append("\nHigh Risk Columns:\n");
            for (Map.Entry<String, ColumnRiskInfo> entry : getHighRiskColumns()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            sb.append("\nRecommendations:\n");
            for (String recommendation : getMitigationRecommendations()) {
                sb.append("  - ").append(recommendation).append("\n");
            }
            
            return sb.toString();
        }
    }
}