package com.privsense.pii.quasiid;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates correlation coefficients between columns to identify potential quasi-identifiers.
 * Uses Pearson correlation for numeric columns and Cramér's V for categorical columns.
 */
@Component
public class ColumnCorrelationCalculator {

    private static final Logger logger = LoggerFactory.getLogger(ColumnCorrelationCalculator.class);

    /**
     * Calculates correlation matrix for all column pairs in the provided map.
     * 
     * @param columnDataMap Map of columns to their sample data
     * @return Map of column pairs to their correlation coefficients
     */
    public Map<ColumnPair, Double> calculateCorrelationMatrix(Map<ColumnInfo, SampleData> columnDataMap) {
        if (columnDataMap == null || columnDataMap.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<ColumnPair, Double> correlationMatrix = new HashMap<>();
        
        // Get all column pairs
        List<ColumnInfo> columns = new ArrayList<>(columnDataMap.keySet());
        for (int i = 0; i < columns.size(); i++) {
            for (int j = i + 1; j < columns.size(); j++) {
                ColumnInfo col1 = columns.get(i);
                ColumnInfo col2 = columns.get(j);
                
                try {
                    // Calculate correlation based on data types
                    double correlation;
                    if (isNumericColumn(col1) && isNumericColumn(col2)) {
                        correlation = calculatePearsonCorrelation(
                            columnDataMap.get(col1), columnDataMap.get(col2));
                    } else {
                        correlation = calculateCramersV(
                            columnDataMap.get(col1), columnDataMap.get(col2));
                    }
                    
                    ColumnPair pair = new ColumnPair(col1, col2);
                    correlationMatrix.put(pair, correlation);
                    
                    logger.debug("Correlation between {} and {}: {}", 
                            col1.getColumnName(), col2.getColumnName(), correlation);
                }
                catch (Exception e) {
                    logger.warn("Error calculating correlation between {} and {}: {}", 
                            col1.getColumnName(), col2.getColumnName(), e.getMessage());
                    
                    // Default to zero correlation on error
                    correlationMatrix.put(new ColumnPair(col1, col2), 0.0);
                }
            }
        }
        
        return correlationMatrix;
    }

    /**
     * Determines if a column is numeric based on its data type.
     */
    private boolean isNumericColumn(ColumnInfo column) {
        if (column == null) {
            return false;
        }
        
        // Use the built-in method to check if the column is numeric
        return column.isNumericType();
    }

    /**
     * Calculates Pearson correlation coefficient between two numeric columns.
     * 
     * @param data1 Sample data from first column
     * @param data2 Sample data from second column
     * @return Correlation coefficient between -1 and 1
     */
    private double calculatePearsonCorrelation(SampleData data1, SampleData data2) {
        // Filter and align numeric values from both samples
        List<double[]> alignedValues = alignNumericValues(data1, data2);
        if (alignedValues.isEmpty()) {
            return 0.0; // Not enough data points
        }
        
        // Extract the arrays
        double[] x = new double[alignedValues.size()];
        double[] y = new double[alignedValues.size()];
        for (int i = 0; i < alignedValues.size(); i++) {
            x[i] = alignedValues.get(i)[0];
            y[i] = alignedValues.get(i)[1];
        }
        
        // Calculate Pearson correlation
        PearsonsCorrelation pearsonCorrelation = new PearsonsCorrelation();
        double correlation;
        try {
            correlation = pearsonCorrelation.correlation(x, y);
            // Handle edge cases like NaN
            if (Double.isNaN(correlation)) {
                correlation = 0.0;
            }
        } catch (Exception e) {
            logger.warn("Error in Pearson correlation calculation: {}", e.getMessage());
            correlation = 0.0;
        }
        
        return correlation;
    }
    
    /**
     * Aligns numeric values from two sample data sets.
     * 
     * @param data1 First sample data set
     * @param data2 Second sample data set
     * @return List of aligned value pairs
     */
    private List<double[]> alignNumericValues(SampleData data1, SampleData data2) {
        List<double[]> result = new ArrayList<>();
        
        // Ensure minimum size for meaningful correlation
        int minSize = Math.min(data1.getSamples().size(), data2.getSamples().size());
        if (minSize < 3) {
            return result;
        }
        
        // Process only the overlapping part
        for (int i = 0; i < minSize; i++) {
            Object v1 = data1.getSamples().get(i);
            Object v2 = data2.getSamples().get(i);
            
            // Try to convert to numeric values
            Double d1 = convertToDouble(v1);
            Double d2 = convertToDouble(v2);
            
            if (d1 != null && d2 != null) {
                result.add(new double[] {d1, d2});
            }
        }
        
        return result;
    }
    
    /**
     * Attempts to convert an object to Double.
     * 
     * @param value The value to convert
     * @return Double value or null if conversion not possible
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Calculates Cramér's V statistic for categorical variables.
     * Cramér's V is based on Pearson's chi-squared test and ranges from 0 to 1.
     * 
     * @param data1 Sample data from first column
     * @param data2 Sample data from second column
     * @return Cramér's V value between 0 and 1
     */
    private double calculateCramersV(SampleData data1, SampleData data2) {
        // Create frequency table (contingency table)
        Map<Object, Map<Object, Integer>> contingencyTable = new HashMap<>();
        
        // Ensure minimum size for meaningful association
        int minSize = Math.min(data1.getSamples().size(), data2.getSamples().size());
        if (minSize < 3) {
            return 0.0;
        }
        
        // Count occurrences of each value combination
        for (int i = 0; i < minSize; i++) {
            Object v1 = data1.getSamples().get(i);
            Object v2 = data2.getSamples().get(i);
            
            // Skip nulls
            if (v1 == null || v2 == null) {
                continue;
            }
            
            // Update contingency table
            contingencyTable.putIfAbsent(v1, new HashMap<>());
            Map<Object, Integer> row = contingencyTable.get(v1);
            row.put(v2, row.getOrDefault(v2, 0) + 1);
        }
        
        // Extract unique values
        Set<Object> uniqueValues1 = contingencyTable.keySet();
        Set<Object> uniqueValues2 = contingencyTable.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
        
        // Insufficient unique values for meaningful analysis
        if (uniqueValues1.size() < 2 || uniqueValues2.size() < 2) {
            return 0.0;
        }
        
        // Create matrix for chi-square test
        int[][] matrix = new int[uniqueValues1.size()][uniqueValues2.size()];
        
        // Map unique values to indices
        Map<Object, Integer> indices1 = mapToIndices(uniqueValues1);
        Map<Object, Integer> indices2 = mapToIndices(uniqueValues2);
        
        // Fill matrix
        for (Map.Entry<Object, Map<Object, Integer>> row : contingencyTable.entrySet()) {
            int rowIndex = indices1.get(row.getKey());
            for (Map.Entry<Object, Integer> cell : row.getValue().entrySet()) {
                int colIndex = indices2.get(cell.getKey());
                matrix[rowIndex][colIndex] = cell.getValue();
            }
        }
        
        // Calculate chi-square statistic
        double chiSquare = calculateChiSquare(matrix);
        
        // Calculate Cramér's V
        int n = Arrays.stream(matrix)
                .flatMapToInt(Arrays::stream)
                .sum();
        
        int minDimension = Math.min(uniqueValues1.size(), uniqueValues2.size()) - 1;
        if (minDimension == 0) {
            return 0.0;
        }
        
        double cramersV = Math.sqrt(chiSquare / (n * minDimension));
        
        // Ensure result is between 0 and 1
        return Math.max(0.0, Math.min(1.0, cramersV));
    }
    
    /**
     * Creates a map of unique values to indices.
     */
    private Map<Object, Integer> mapToIndices(Set<Object> values) {
        Map<Object, Integer> indices = new HashMap<>();
        int i = 0;
        for (Object value : values) {
            indices.put(value, i++);
        }
        return indices;
    }
    
    /**
     * Calculates chi-square statistic for a contingency table.
     */
    private double calculateChiSquare(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        
        // Calculate row and column totals
        int[] rowTotals = new int[rows];
        int[] colTotals = new int[cols];
        int total = 0;
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rowTotals[i] += matrix[i][j];
                colTotals[j] += matrix[i][j];
                total += matrix[i][j];
            }
        }
        
        // Calculate chi-square
        double chiSquare = 0.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (rowTotals[i] > 0 && colTotals[j] > 0) {
                    double expected = (double) rowTotals[i] * colTotals[j] / total;
                    double diff = matrix[i][j] - expected;
                    chiSquare += (diff * diff) / expected;
                }
            }
        }
        
        return chiSquare;
    }

    /**
     * Represents a pair of columns for correlation calculation.
     */
    public static class ColumnPair {
        private final ColumnInfo col1;
        private final ColumnInfo col2;
        
        public ColumnPair(ColumnInfo col1, ColumnInfo col2) {
            // Always store columns in consistent order to support lookup
            if (col1.getColumnName().compareTo(col2.getColumnName()) <= 0) {
                this.col1 = col1;
                this.col2 = col2;
            } else {
                this.col1 = col2;
                this.col2 = col1;
            }
        }
        
        public ColumnInfo getCol1() {
            return col1;
        }
        
        public ColumnInfo getCol2() {
            return col2;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnPair that = (ColumnPair) o;
            return Objects.equals(col1, that.col1) && 
                   Objects.equals(col2, that.col2);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(col1, col2);
        }
    }
}