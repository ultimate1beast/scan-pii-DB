package com.privsense.api.service.impl;

import com.privsense.core.service.PiiDetectionProgressCallback;
import com.privsense.core.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket-based implementation of PII detection progress callback.
 * Sends real-time progress updates to connected clients via WebSocket.
 */
@Component
public class WebSocketPiiDetectionProgressCallback implements PiiDetectionProgressCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketPiiDetectionProgressCallback.class);    // Constants for notification data keys
    private static final String COLUMN_NAME = "columnName";
    private static final String COLUMN_INDEX = "columnIndex";
    private static final String TOTAL_COLUMNS = "totalColumns";
    private static final String PROGRESS = "progress";
    private static final String CURRENT_OPERATION = "currentOperation";
    
    // Constants for notification types
    private static final String NOTIFICATION_TYPE_PROGRESS = "progress";
    private static final String NOTIFICATION_TYPE_QI = "quasi-identifiers";
    private static final String NOTIFICATION_TYPE_STATUS = "scan-status";
    private static final String NOTIFICATION_TYPE_RESULTS = "results";
    
    private final NotificationService notificationService;
    private String currentJobId;
    
    public WebSocketPiiDetectionProgressCallback(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Set the current job ID for this callback instance
     */
    public void setJobId(String jobId) {
        this.currentJobId = jobId;
    }
      @Override
    public void onColumnProcessingStarted(String columnName, int columnIndex, int totalColumns) {
        if (currentJobId == null) return;
        
        logger.debug("Column processing started: {} ({}/{})", columnName, columnIndex + 1, totalColumns);
        
        Map<String, Object> progressData = createBaseProgressData("COLUMN_PROCESSING_STARTED");
        progressData.put(COLUMN_NAME, columnName);
        progressData.put(COLUMN_INDEX, columnIndex);
        progressData.put(TOTAL_COLUMNS, totalColumns);
        progressData.put(PROGRESS, calculateProgress(columnIndex, totalColumns));
        progressData.put(CURRENT_OPERATION, "Analyzing column: " + columnName);
        
        sendProgressNotification(progressData);
    }
    
    @Override
    public void onPiiDetected(String columnName, String piiType, double confidence, String detectionMethod,
                             int columnIndex, int totalColumns, String sampleData) {        if (currentJobId == null) return;
        
        logger.info("PII detected in column {}: {} (confidence: {}, method: {})", 
                   columnName, piiType, confidence, detectionMethod);
        
        Map<String, Object> piiData = createBaseProgressData("PII_DETECTED");
        piiData.put(COLUMN_NAME, columnName);
        piiData.put("piiType", piiType);
        piiData.put("confidence", confidence);
        piiData.put("detectionMethod", detectionMethod);
        piiData.put(COLUMN_INDEX, columnIndex);
        piiData.put(TOTAL_COLUMNS, totalColumns);
        piiData.put("sampleData", maskSensitiveData(sampleData));
        piiData.put("sensitiveData", "Sample: " + maskSensitiveData(sampleData));
        
        sendProgressNotification(piiData);
    }
    
    @Override
    public void onColumnProcessingCompleted(String columnName, int columnIndex, int totalColumns, 
                                          boolean hasPii, int piiCount) {
        if (currentJobId == null) return;
        
        logger.debug("Column processing completed: {} - PII found: {} (count: {})", 
                    columnName, hasPii, piiCount);
          Map<String, Object> completionData = createBaseProgressData("COLUMN_PROCESSING_COMPLETED");
        completionData.put(COLUMN_NAME, columnName);
        completionData.put(COLUMN_INDEX, columnIndex);
        completionData.put(TOTAL_COLUMNS, totalColumns);
        completionData.put("hasPii", hasPii);
        completionData.put("piiCount", piiCount);
        completionData.put(PROGRESS, calculateProgress(columnIndex + 1, totalColumns));
        completionData.put("columnsProcessed", columnIndex + 1);
        
        sendProgressNotification(completionData);
    }
    
    @Override
    public void onQuasiIdentifierAnalysisStarted(int totalEligibleColumns) {
        if (currentJobId == null) return;
        
        logger.info("Starting quasi-identifier analysis for {} eligible columns", totalEligibleColumns);
          Map<String, Object> qiStartData = createBaseProgressData("QI_ANALYSIS_STARTED");
        qiStartData.put("totalEligibleColumns", totalEligibleColumns);
        qiStartData.put(CURRENT_OPERATION, "Analyzing quasi-identifiers");
        
        sendProgressNotification(qiStartData);
        
        // Also send phase change notification
        onScanPhaseChanged("QUASI_IDENTIFIER_ANALYSIS", "Analyzing quasi-identifiers and correlations");
    }
    
    @Override
    public void onQuasiIdentifierDetected(String columnName, String qiGroup, String qiType, 
                                        double riskScore, List<String> correlatedColumns) {        if (currentJobId == null) return;
        
        logger.info("Quasi-identifier detected: {} in group {} (risk: {})", 
                   columnName, qiGroup, riskScore);
        
        Map<String, Object> qiData = createBaseProgressData("QUASI_IDENTIFIER_DETECTED");
        qiData.put(COLUMN_NAME, columnName);
        qiData.put("qiGroup", qiGroup);
        qiData.put("qiType", qiType);
        qiData.put("riskScore", riskScore);
        qiData.put("confidence", riskScore);        qiData.put("correlatedColumns", correlatedColumns);
        qiData.put("correlationCount", correlatedColumns.size());
        
        sendQuasiIdentifierNotification(qiData);
    }
    
    @Override
    public void onQuasiIdentifierGroupFormed(String groupName, List<String> columns, 
                                           double groupRisk, String detectionMethod) {        if (currentJobId == null) return;
        
        logger.info("QI group formed: {} with {} columns (risk: {})", 
                   groupName, columns.size(), groupRisk);
        
        Map<String, Object> groupData = createBaseProgressData("QI_GROUP_FORMED");
        groupData.put("groupName", groupName);
        groupData.put("columns", columns);
        groupData.put("columnCount", columns.size());
        groupData.put("groupRisk", groupRisk);
        groupData.put("detectionMethod", detectionMethod);
        
        sendQuasiIdentifierNotification(groupData);
    }
    
    @Override
    public void onQuasiIdentifierAnalysisCompleted(int totalQiColumns, int totalQiGroups) {
        if (currentJobId == null) return;
        
        logger.info("QI analysis completed: {} QI columns in {} groups", totalQiColumns, totalQiGroups);
          Map<String, Object> qiCompletionData = createBaseProgressData("QI_ANALYSIS_COMPLETED");
        qiCompletionData.put("totalQiColumns", totalQiColumns);
        qiCompletionData.put("totalQiGroups", totalQiGroups);
        qiCompletionData.put(CURRENT_OPERATION, "Quasi-identifier analysis completed");
        
        sendProgressNotification(qiCompletionData);
    }
    
    @Override
    public void onScanPhaseChanged(String phase, String message) {
        if (currentJobId == null) return;
        
        logger.info("Scan phase changed: {} - {}", phase, message);
          Map<String, Object> phaseData = createBaseProgressData("SCAN_PHASE_CHANGED");
        phaseData.put("phase", phase);
        phaseData.put("message", message);
        phaseData.put(CURRENT_OPERATION, message);
        
        sendStatusNotification(phaseData);
    }
    
    @Override
    public void onOverallProgressUpdated(double overallProgress, String currentOperation, 
                                       int columnsProcessed, int totalColumns) {        if (currentJobId == null) return;
        
        Map<String, Object> progressData = createBaseProgressData("OVERALL_PROGRESS_UPDATED");
        progressData.put("overallProgress", overallProgress);
        progressData.put(CURRENT_OPERATION, currentOperation);
        progressData.put("columnsProcessed", columnsProcessed);
        progressData.put(TOTAL_COLUMNS, totalColumns);
        progressData.put(PROGRESS, overallProgress);
        
        sendProgressNotification(progressData);
    }
    
    @Override
    public void onScanCompleted(int totalColumns, int piiColumns, int qiColumns, 
                              Set<String> piiTypes, int qiGroups, double complianceScore) {
        if (currentJobId == null) return;
        
        logger.info("Scan completed: {}/{} columns have PII, {} QI columns in {} groups", 
                   piiColumns, totalColumns, qiColumns, qiGroups);
          Map<String, Object> finalData = createBaseProgressData("SCAN_COMPLETED");
        finalData.put(TOTAL_COLUMNS, totalColumns);
        finalData.put("totalPiiColumns", piiColumns);
        finalData.put("totalQiColumns", qiColumns);
        finalData.put("piiTypes", piiTypes);
        finalData.put("qiGroups", qiGroups);
        finalData.put("complianceScore", complianceScore);
        finalData.put("overallProgress", 100.0);
        finalData.put(CURRENT_OPERATION, "Scan completed successfully");
        finalData.put("recommendations", generateRecommendations(piiColumns, qiColumns, totalColumns));
        
        sendResultsNotification(finalData);
    }
    
    // Helper methods
    
    private Map<String, Object> createBaseProgressData(String eventType) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", eventType);
        data.put("jobId", currentJobId);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
    
    private double calculateProgress(int current, int total) {
        if (total == 0) return 0.0;
        return ((double) current / total) * 100.0;
    }
    
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "***";
        }
        return data.substring(0, 2) + "***" + data.substring(data.length() - 2);
    }
    
    private List<String> generateRecommendations(int piiColumns, int qiColumns, int totalColumns) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        if (piiColumns > 0) {
            recommendations.add("Consider implementing encryption for " + piiColumns + " PII columns");
            recommendations.add("Review data access permissions for sensitive columns");
        }
        
        if (qiColumns > 0) {
            recommendations.add("Apply data anonymization techniques to " + qiColumns + " quasi-identifier columns");
            recommendations.add("Consider k-anonymity or differential privacy methods");
        }
        
        double sensitivityRatio = (double) (piiColumns + qiColumns) / totalColumns;
        if (sensitivityRatio > 0.3) {
            recommendations.add("High sensitivity detected - consider data minimization strategies");
        }
        
        return recommendations;
    }
      private void sendProgressNotification(Map<String, Object> data) {
        try {
            notificationService.sendJobNotification(currentJobId, NOTIFICATION_TYPE_PROGRESS, data);
        } catch (Exception e) {
            logger.error("Failed to send progress notification for job {}: {}", currentJobId, e.getMessage());
        }
    }
      private void sendQuasiIdentifierNotification(Map<String, Object> data) {
        try {
            notificationService.sendJobNotification(currentJobId, NOTIFICATION_TYPE_QI, data);
        } catch (Exception e) {
            logger.error("Failed to send QI notification for job {}: {}", currentJobId, e.getMessage());
        }
    }
      private void sendStatusNotification(Map<String, Object> data) {
        try {
            notificationService.sendJobNotification(currentJobId, NOTIFICATION_TYPE_STATUS, data);
        } catch (Exception e) {
            logger.error("Failed to send status notification for job {}: {}", currentJobId, e.getMessage());
        }
    }
      private void sendResultsNotification(Map<String, Object> data) {
        try {
            notificationService.sendJobNotification(currentJobId, NOTIFICATION_TYPE_RESULTS, data);
        } catch (Exception e) {
            logger.error("Failed to send results notification for job {}: {}", currentJobId, e.getMessage());
        }
    }
}