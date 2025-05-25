package com.privsense.core.service;

/**
 * Callback interface for reporting PII detection progress during column-by-column processing.
 * This enables real-time WebSocket notifications to clients showing scan progress.
 */
public interface PiiDetectionProgressCallback {
    
    /**
     * Called when starting to process a specific column
     * 
     * @param columnName The name of the column being processed
     * @param columnIndex The 0-based index of the current column
     * @param totalColumns The total number of columns to process
     */
    void onColumnProcessingStarted(String columnName, int columnIndex, int totalColumns);
    
    /**
     * Called when PII is detected in a column
     * 
     * @param columnName The name of the column where PII was detected
     * @param piiType The type of PII detected (e.g., "EMAIL", "PHONE", "SSN")
     * @param confidence The confidence score of the detection (0.0 to 1.0)
     * @param detectionMethod The method used for detection (e.g., "REGEX", "NER", "HEURISTIC")
     * @param columnIndex The 0-based index of the current column
     * @param totalColumns The total number of columns to process
     * @param sampleData Sample of the detected PII data (masked for security)
     */
    void onPiiDetected(String columnName, String piiType, double confidence, String detectionMethod, 
                      int columnIndex, int totalColumns, String sampleData);
    
    /**
     * Called when a column has been fully processed (whether PII was found or not)
     * 
     * @param columnName The name of the column that was processed
     * @param columnIndex The 0-based index of the current column
     * @param totalColumns The total number of columns to process
     * @param hasPii Whether PII was detected in this column
     * @param piiCount Number of PII types detected in this column
     */
    void onColumnProcessingCompleted(String columnName, int columnIndex, int totalColumns, 
                                   boolean hasPii, int piiCount);
    
    /**
     * Called when quasi-identifier detection begins
     * 
     * @param totalEligibleColumns Number of columns eligible for QI analysis
     */
    void onQuasiIdentifierAnalysisStarted(int totalEligibleColumns);
    
    /**
     * Called when a quasi-identifier is detected
     * 
     * @param columnName The name of the column identified as QI
     * @param qiGroup The quasi-identifier group name
     * @param qiType The type of quasi-identifier (e.g., "DEMOGRAPHIC", "GEOGRAPHIC")
     * @param riskScore The privacy risk score (0.0 to 1.0)
     * @param correlatedColumns List of columns correlated with this QI
     */
    void onQuasiIdentifierDetected(String columnName, String qiGroup, String qiType, 
                                 double riskScore, java.util.List<String> correlatedColumns);
    
    /**
     * Called when a quasi-identifier group is formed
     * 
     * @param groupName The name of the QI group
     * @param columns List of columns in the group
     * @param groupRisk Overall privacy risk score for the group
     * @param detectionMethod The method used to detect the QI group
     */
    void onQuasiIdentifierGroupFormed(String groupName, java.util.List<String> columns, 
                                    double groupRisk, String detectionMethod);
    
    /**
     * Called when quasi-identifier analysis is completed
     * 
     * @param totalQiColumns Total number of QI columns found
     * @param totalQiGroups Total number of QI groups formed
     */
    void onQuasiIdentifierAnalysisCompleted(int totalQiColumns, int totalQiGroups);
    
    /**
     * Called when scan phase changes
     * 
     * @param phase The current scan phase
     * @param message Descriptive message about the phase
     */
    void onScanPhaseChanged(String phase, String message);
    
    /**
     * Called when overall scan progress updates
     * 
     * @param overallProgress Overall scan progress percentage (0.0 to 100.0)
     * @param currentOperation Description of current operation
     * @param columnsProcessed Number of columns processed so far
     * @param totalColumns Total number of columns to process
     */
    void onOverallProgressUpdated(double overallProgress, String currentOperation, 
                                int columnsProcessed, int totalColumns);
    
    /**
     * Called when final scan results are available
     * 
     * @param totalColumns Total number of columns scanned
     * @param piiColumns Number of columns with PII
     * @param qiColumns Number of quasi-identifier columns
     * @param piiTypes Set of PII types found
     * @param qiGroups Number of QI groups formed
     * @param complianceScore Overall compliance score
     */
    void onScanCompleted(int totalColumns, int piiColumns, int qiColumns, 
                        java.util.Set<String> piiTypes, int qiGroups, double complianceScore);
}