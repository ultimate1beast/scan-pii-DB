package com.privsense.pii.quasiid;

import com.privsense.core.config.QuasiIdentifierConfig;
import com.privsense.core.model.*;
import org.apache.commons.math3.ml.clustering.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements clustering-based detection of correlated quasi-identifier columns.
 * Uses graph theory and optionally DBSCAN clustering to identify column groups.
 */
@Component
public class ClusteringBasedQuasiIdentifierDetector {

    private static final Logger logger = LoggerFactory.getLogger(ClusteringBasedQuasiIdentifierDetector.class);

    private final ColumnValueDistributionAnalyzer distributionAnalyzer;
    private final ColumnCorrelationCalculator correlationCalculator;
    private final QuasiIdentifierConfig config;

    @Autowired
    public ClusteringBasedQuasiIdentifierDetector(
            ColumnValueDistributionAnalyzer distributionAnalyzer,
            ColumnCorrelationCalculator correlationCalculator,
            QuasiIdentifierConfig config) {
        this.distributionAnalyzer = distributionAnalyzer;
        this.correlationCalculator = correlationCalculator;
        this.config = config;
    }

    /**
     * Detects correlated column groups that could form quasi-identifiers.
     *
     * @param columnDataMap Map of column info to sample data
     * @return List of identified correlated quasi-identifier groups
     */
    public List<CorrelatedQuasiIdentifierGroup> detectCorrelatedColumns(Map<ColumnInfo, SampleData> columnDataMap) {
        if (columnDataMap == null || columnDataMap.isEmpty()) {
            return Collections.emptyList();
        }
        
        logger.info("Starting quasi-identifier detection for {} columns", columnDataMap.size());
        
        // 1. Filter columns based on distribution metrics
        Map<ColumnInfo, SampleData> filteredColumnMap = filterEligibleColumns(columnDataMap);
        if (filteredColumnMap.isEmpty()) {
            logger.info("No eligible columns for quasi-identifier detection after filtering");
            return Collections.emptyList();
        }
        
        logger.debug("Filtered to {} eligible columns for QI detection", filteredColumnMap.size());
        
        // 2. Calculate correlation matrix between columns
        Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix = 
                correlationCalculator.calculateCorrelationMatrix(filteredColumnMap);
        
        // 3. Build graph or use clustering based on configuration
        List<Set<ColumnInfo>> columnGroups;
        
        if (config.isUseMachineLearning()) {
            logger.info("Using machine learning clustering for quasi-identifier detection");
            columnGroups = identifyColumnGroupsWithClustering(correlationMatrix, filteredColumnMap.keySet());
        } else {
            logger.info("Using graph-based clustering for quasi-identifier detection");
            columnGroups = identifyColumnGroupsWithGraph(correlationMatrix, filteredColumnMap.keySet());
        }
        
        logger.debug("Initial QI grouping found {} potential groups", columnGroups.size());
        
        // 4. Convert to result objects with risk assessments
        List<CorrelatedQuasiIdentifierGroup> groups = convertToQuasiIdentifierGroups(columnGroups, filteredColumnMap);
        
        return groups;
    }
    
    /**
     * Filters columns based on distribution characteristics to find potential QI columns.
     */
    private Map<ColumnInfo, SampleData> filterEligibleColumns(Map<ColumnInfo, SampleData> columnDataMap) {
        Map<ColumnInfo, SampleData> filteredMap = new HashMap<>();
        Map<ColumnInfo, ColumnValueDistributionAnalyzer.ColumnDistributionMetrics> metricsMap = new HashMap<>();
        
        // Calculate metrics for all columns
        for (Map.Entry<ColumnInfo, SampleData> entry : columnDataMap.entrySet()) {
            ColumnInfo column = entry.getKey();
            SampleData sampleData = entry.getValue();
            
            ColumnValueDistributionAnalyzer.ColumnDistributionMetrics metrics = 
                    distributionAnalyzer.calculateDistribution(column, sampleData);
                    
            metricsMap.put(column, metrics);
            
            // Apply filtering criteria:
            // 1. Minimum number of distinct values
            // 2. Not too many distinct values (maxDistinctValueRatio)
            // 3. Minimum entropy threshold
            boolean hasEnoughValues = metrics.getDistinctValueCount() >= config.getMinDistinctValueCount();
            boolean notTooManyValues = metrics.getDistinctValueRatio() <= config.getMaxDistinctValueRatio();
            boolean hasEnoughEntropy = metrics.getEntropy() >= config.getEntropyThreshold();
            
            if (hasEnoughValues && notTooManyValues && hasEnoughEntropy) {
                filteredMap.put(column, sampleData);
                logger.debug("Column {} is eligible for QI detection (values={}, ratio={}, entropy={})", 
                        column.getColumnName(), metrics.getDistinctValueCount(), 
                        metrics.getDistinctValueRatio(), metrics.getEntropy());
            }
        }
        
        return filteredMap;
    }
    
    /**
     * Identifies column groups using graph-based connected components.
     */
    private List<Set<ColumnInfo>> identifyColumnGroupsWithGraph(
            Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix, 
            Set<ColumnInfo> columns) {
        
        // Build adjacency map based on correlation threshold
        Map<ColumnInfo, Set<ColumnInfo>> adjacencyMap = new HashMap<>();
        for (ColumnInfo column : columns) {
            adjacencyMap.put(column, new HashSet<>());
        }
        
        // Add edges where correlation exceeds threshold
        double threshold = config.getCorrelationThreshold();
        int edgesAdded = 0;
        
        for (Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double> entry : correlationMatrix.entrySet()) {
            if (entry.getValue() >= threshold) {
                ColumnCorrelationCalculator.ColumnPair pair = entry.getKey();
                ColumnInfo col1 = pair.getCol1();
                ColumnInfo col2 = pair.getCol2();
                
                adjacencyMap.get(col1).add(col2);
                adjacencyMap.get(col2).add(col1);
                edgesAdded++;
                
                logger.debug("Added edge between {} and {} with correlation {}", 
                        col1.getColumnName(), col2.getColumnName(), entry.getValue());
            }
        }
        
        logger.debug("Built correlation graph with {} edges (threshold={})", edgesAdded, threshold);
        
        // If no edges added with current threshold, try with a lower threshold
        if (edgesAdded == 0 && columns.size() >= 2) {
            double relaxedThreshold = Math.max(0.5, threshold - 0.1);
            logger.debug("No edges found with threshold {}, trying relaxed threshold {}", threshold, relaxedThreshold);
            
            for (Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double> entry : correlationMatrix.entrySet()) {
                if (entry.getValue() >= relaxedThreshold) {
                    ColumnCorrelationCalculator.ColumnPair pair = entry.getKey();
                    ColumnInfo col1 = pair.getCol1();
                    ColumnInfo col2 = pair.getCol2();
                    
                    adjacencyMap.get(col1).add(col2);
                    adjacencyMap.get(col2).add(col1);
                    edgesAdded++;
                    
                    logger.debug("Added edge (relaxed) between {} and {} with correlation {}", 
                            col1.getColumnName(), col2.getColumnName(), entry.getValue());
                }
            }
            
            logger.debug("Built correlation graph with {} edges after threshold relaxation", edgesAdded);
        }
        
        // Find connected components (column groups)
        List<Set<ColumnInfo>> connectedComponents = findConnectedComponents(adjacencyMap);
        logger.debug("Found {} connected components before size filtering", connectedComponents.size());

        // Instead of simply filtering out large components, break them into appropriate sized groups
        List<Set<ColumnInfo>> appropriateSizedGroups = new ArrayList<>();
        
        for (Set<ColumnInfo> component : connectedComponents) {
            if (component.size() < config.getMinGroupSize()) {
                // Skip groups that are too small
                logger.debug("Skipping component with {} columns - too small", component.size());
                continue;
            } else if (component.size() <= config.getMaxGroupSize()) {
                // Component is already the right size
                appropriateSizedGroups.add(component);
                logger.debug("Added component with {} columns directly", component.size());
            } else {
                // Component is too large, break it into subgroups
                List<Set<ColumnInfo>> subgroups = breakIntoSubgroups(component, adjacencyMap, correlationMatrix);
                appropriateSizedGroups.addAll(subgroups);
                logger.debug("Broke large component with {} columns into {} subgroups", 
                        component.size(), subgroups.size());
            }
        }
        
        logger.debug("Final component count after size adjustments: {}", appropriateSizedGroups.size());
        
        // If we still have no valid groups, fall back to pairs
        if (appropriateSizedGroups.isEmpty() && !correlationMatrix.isEmpty()) {
            logger.debug("No valid components found, creating pairs from highest correlations");
            return createPairsFromHighestCorrelations(correlationMatrix);
        }
        
        return appropriateSizedGroups;
    }
    
    /**
     * Breaks a large connected component into smaller subgroups of appropriate size.
     * Uses core-periphery decomposition based on connection density.
     */
    private List<Set<ColumnInfo>> breakIntoSubgroups(
            Set<ColumnInfo> largeComponent, 
            Map<ColumnInfo, Set<ColumnInfo>> adjacencyMap,
            Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix) {
        
        List<Set<ColumnInfo>> subgroups = new ArrayList<>();
        
        // First try to identify clusters by edge weight
        Map<ColumnInfo, Map<ColumnInfo, Double>> weightedAdjacencyMap = new HashMap<>();
        
        // Build weighted adjacency map
        for (ColumnInfo column : largeComponent) {
            weightedAdjacencyMap.put(column, new HashMap<>());
            for (ColumnInfo neighbor : adjacencyMap.get(column)) {
                if (largeComponent.contains(neighbor)) {
                    ColumnCorrelationCalculator.ColumnPair pair = 
                            new ColumnCorrelationCalculator.ColumnPair(column, neighbor);
                    double weight = correlationMatrix.getOrDefault(pair, 0.0);
                    weightedAdjacencyMap.get(column).put(neighbor, weight);
                }
            }
        }
        
        // Identify dense subgroups using node degree and edge weights
        List<ColumnInfo> sortedColumns = new ArrayList<>(largeComponent);
        
        // Sort columns by their connectivity (weighted degree)
        sortedColumns.sort((c1, c2) -> {
            double w1 = weightedAdjacencyMap.get(c1).values().stream().mapToDouble(Double::doubleValue).sum();
            double w2 = weightedAdjacencyMap.get(c2).values().stream().mapToDouble(Double::doubleValue).sum();
            return Double.compare(w2, w1); // Descending order
        });
        
        // Create initial seed groups from highly connected columns
        int maxSize = config.getMaxGroupSize();
        Set<ColumnInfo> used = new HashSet<>();
        
        // Process columns in order of connectivity
        for (ColumnInfo seedColumn : sortedColumns) {
            if (used.contains(seedColumn)) continue;
            
            // Start a new group with this seed column
            Set<ColumnInfo> group = new HashSet<>();
            group.add(seedColumn);
            used.add(seedColumn);
            
            // Sort neighbors by correlation strength
            List<Map.Entry<ColumnInfo, Double>> sortedNeighbors = 
                    weightedAdjacencyMap.get(seedColumn).entrySet().stream()
                    .sorted(Map.Entry.<ColumnInfo, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());
            
            // Add strongest connections to the group until we reach maxSize
            for (Map.Entry<ColumnInfo, Double> entry : sortedNeighbors) {
                ColumnInfo neighbor = entry.getKey();
                if (!used.contains(neighbor) && group.size() < maxSize) {
                    group.add(neighbor);
                    used.add(neighbor);
                }
            }
            
            // Only add groups that meet the minimum size requirement
            if (group.size() >= config.getMinGroupSize()) {
                subgroups.add(group);
                logger.debug("Created subgroup with {} columns from large component", group.size());
            }
            
            // If we've used all columns, break
            if (used.size() == largeComponent.size()) break;
        }
        
        // If we didn't form any valid subgroups, create pairs from highest correlations within this component
        if (subgroups.isEmpty()) {
            logger.debug("No valid subgroups formed, creating pairs from component with {} columns", largeComponent.size());
            
            // Get correlation pairs involving only columns in this component
            Map<ColumnCorrelationCalculator.ColumnPair, Double> componentCorrelations = new HashMap<>();
            for (Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double> entry : correlationMatrix.entrySet()) {
                ColumnInfo col1 = entry.getKey().getCol1();
                ColumnInfo col2 = entry.getKey().getCol2();
                if (largeComponent.contains(col1) && largeComponent.contains(col2)) {
                    componentCorrelations.put(entry.getKey(), entry.getValue());
                }
            }
            
            // Create pairs from this component's correlations
            used.clear();
            List<Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double>> sortedCorrelations =
                    componentCorrelations.entrySet().stream()
                    .sorted(Map.Entry.<ColumnCorrelationCalculator.ColumnPair, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());
            
            for (Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double> entry : sortedCorrelations) {
                ColumnInfo col1 = entry.getKey().getCol1();
                ColumnInfo col2 = entry.getKey().getCol2();
                
                // If neither column is used yet, create a pair
                if (!used.contains(col1) && !used.contains(col2)) {
                    Set<ColumnInfo> pair = new HashSet<>();
                    pair.add(col1);
                    pair.add(col2);
                    subgroups.add(pair);
                    
                    used.add(col1);
                    used.add(col2);
                    
                    logger.debug("Created pair from large component: {} and {} (correlation={})",
                            col1.getColumnName(), col2.getColumnName(), entry.getValue());
                }
                
                // If all columns are used, stop
                if (used.size() >= largeComponent.size()) break;
            }
        }
        
        // Attempt to create triplets from remaining pairs and unused columns
        expandGroupsToTriplets(subgroups, correlationMatrix, used, largeComponent);
        
        return subgroups;
    }
    
    /**
     * Tries to expand pairs into triplets by adding a third correlated column.
     */
    private void expandGroupsToTriplets(
            List<Set<ColumnInfo>> groups, 
            Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix,
            Set<ColumnInfo> usedColumns,
            Set<ColumnInfo> component) {
        
        // Get pairs that could be expanded
        List<Set<ColumnInfo>> pairsToExpand = groups.stream()
                .filter(g -> g.size() == 2)
                .collect(Collectors.toList());
        
        // Get unused columns
        Set<ColumnInfo> unused = new HashSet<>(component);
        unused.removeAll(usedColumns);
        
        if (unused.isEmpty() || pairsToExpand.isEmpty()) return;
        
        // Try to expand each pair into a triplet
        for (Set<ColumnInfo> pair : pairsToExpand) {
            ColumnInfo[] columns = pair.toArray(new ColumnInfo[0]);
            ColumnInfo col1 = columns[0];
            ColumnInfo col2 = columns[1];
            
            // Find best third column with strong correlation to both existing columns
            ColumnInfo bestThird = null;
            double bestAvgCorrelation = 0.0;
            
            for (ColumnInfo candidate : unused) {
                ColumnCorrelationCalculator.ColumnPair pair1 = 
                        new ColumnCorrelationCalculator.ColumnPair(col1, candidate);
                ColumnCorrelationCalculator.ColumnPair pair2 = 
                        new ColumnCorrelationCalculator.ColumnPair(col2, candidate);
                
                double corr1 = correlationMatrix.getOrDefault(pair1, 0.0);
                double corr2 = correlationMatrix.getOrDefault(pair2, 0.0);
                
                // Both correlations must be above threshold
                if (corr1 >= config.getCorrelationThreshold() && corr2 >= config.getCorrelationThreshold()) {
                    double avgCorr = (corr1 + corr2) / 2.0;
                    if (avgCorr > bestAvgCorrelation) {
                        bestAvgCorrelation = avgCorr;
                        bestThird = candidate;
                    }
                }
            }
            
            // If we found a good third column, add it to the pair
            if (bestThird != null) {
                pair.add(bestThird);
                unused.remove(bestThird);
                usedColumns.add(bestThird);
                logger.debug("Expanded pair to triplet by adding {} (avg correlation: {})",
                        bestThird.getColumnName(), bestAvgCorrelation);
            }
        }
    }
    
    /**
     * Creates column pairs from highest correlations when connected component approach doesn't find groups.
     */
    private List<Set<ColumnInfo>> createPairsFromHighestCorrelations(
            Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix) {
        
        // Sort correlations from highest to lowest
        List<Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double>> sortedCorrelations = 
                correlationMatrix.entrySet().stream()
                        .filter(e -> e.getValue() >= config.getCorrelationThreshold() * 0.8) // Use 80% of threshold
                        .sorted(Map.Entry.<ColumnCorrelationCalculator.ColumnPair, Double>comparingByValue().reversed())
                        .toList();
        
        List<Set<ColumnInfo>> pairs = new ArrayList<>();
        Set<ColumnInfo> usedColumns = new HashSet<>();
        
        // Create pairs from highest correlations
        for (Map.Entry<ColumnCorrelationCalculator.ColumnPair, Double> entry : sortedCorrelations) {
            ColumnCorrelationCalculator.ColumnPair pair = entry.getKey();
            ColumnInfo col1 = pair.getCol1();
            ColumnInfo col2 = pair.getCol2();
            
            // If we can use both columns
            if (!usedColumns.contains(col1) && !usedColumns.contains(col2)) {
                Set<ColumnInfo> newPair = new HashSet<>();
                newPair.add(col1);
                newPair.add(col2);
                pairs.add(newPair);
                
                usedColumns.add(col1);
                usedColumns.add(col2);
                
                logger.debug("Created QI pair from high correlation: {} and {} (correlation={})",
                        col1.getColumnName(), col2.getColumnName(), entry.getValue());
            }
            
            // Limit to a reasonable number of pairs
            if (pairs.size() >= 5) {
                break;
            }
        }
        
        return pairs;
    }
    
    /**
     * Finds connected components in a graph using breadth-first search.
     */
    private List<Set<ColumnInfo>> findConnectedComponents(Map<ColumnInfo, Set<ColumnInfo>> adjacencyMap) {
        List<Set<ColumnInfo>> components = new ArrayList<>();
        Set<ColumnInfo> visited = new HashSet<>();
        
        for (ColumnInfo column : adjacencyMap.keySet()) {
            if (!visited.contains(column)) {
                // Found a new potential component
                Set<ColumnInfo> component = new HashSet<>();
                Queue<ColumnInfo> queue = new LinkedList<>();
                
                queue.add(column);
                visited.add(column);
                component.add(column);
                
                // BFS to find all connected nodes
                while (!queue.isEmpty()) {
                    ColumnInfo current = queue.poll();
                    for (ColumnInfo neighbor : adjacencyMap.get(current)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            component.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                
                // Only add components with at least one edge
                boolean hasEdges = component.stream()
                        .anyMatch(col -> !adjacencyMap.get(col).isEmpty());
                
                if (hasEdges || component.size() > 1) {
                    components.add(component);
                    logger.debug("Found connected component with {} columns", component.size());
                }
            }
        }
        
        return components;
    }
    
    /**
     * Identifies column groups using DBSCAN clustering based on correlations.
     */
    private List<Set<ColumnInfo>> identifyColumnGroupsWithClustering(
            Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix, 
            Set<ColumnInfo> columns) {
        
        // Skip clustering if not enough columns
        if (columns.size() < config.getMinGroupSize()) {
            return Collections.emptyList();
        }
        
        // Create distance matrix from correlations
        Map<ColumnInfo, Map<ColumnInfo, Double>> distanceMap = new HashMap<>();
        for (ColumnInfo col1 : columns) {
            distanceMap.put(col1, new HashMap<>());
            for (ColumnInfo col2 : columns) {
                if (col1 == col2) {
                    distanceMap.get(col1).put(col2, 0.0);
                } else {
                    // Convert correlation to distance (1 - correlation)
                    ColumnCorrelationCalculator.ColumnPair pair = 
                            new ColumnCorrelationCalculator.ColumnPair(col1, col2);
                    double correlation = correlationMatrix.getOrDefault(pair, 0.0);
                    double distance = 1.0 - Math.abs(correlation); // Use absolute correlation value
                    distanceMap.get(col1).put(col2, distance);
                }
            }
        }
        
        // Create points for DBSCAN
        List<ColumnClusterPoint> points = new ArrayList<>();
        for (ColumnInfo column : columns) {
            points.add(new ColumnClusterPoint(column, distanceMap));
        }
        
        // Create and run DBSCAN with adjusted distance threshold
        // Use a more relaxed threshold that will form clusters with high correlation values
        double adjustedThreshold = Math.min(0.5, config.getClusteringDistanceThreshold() * 1.5);
        
        logger.debug("Running DBSCAN with adjusted threshold: {}", adjustedThreshold);
        
        DBSCANClusterer<ColumnClusterPoint> clusterer = 
                new DBSCANClusterer<>(adjustedThreshold, config.getMinGroupSize());
        List<Cluster<ColumnClusterPoint>> clusters = clusterer.cluster(points);
        
        // If no clusters found, try again with a more relaxed threshold
        if (clusters.isEmpty() && columns.size() >= 3) {
            adjustedThreshold = 0.6; // Allow columns with correlation >= 0.4 to cluster
            logger.debug("No clusters found, retrying with relaxed threshold: {}", adjustedThreshold);
            
            clusterer = new DBSCANClusterer<>(adjustedThreshold, config.getMinGroupSize());
            clusters = clusterer.cluster(points);
        }
        
        // Convert clusters to column groups
        List<Set<ColumnInfo>> columnGroups = clusters.stream()
                .map(cluster -> cluster.getPoints().stream()
                        .map(ColumnClusterPoint::getColumn)
                        .collect(Collectors.toSet()))
                .filter(group -> group.size() >= config.getMinGroupSize() && group.size() <= config.getMaxGroupSize())
                .toList();
                
        logger.debug("DBSCAN clustering identified {} column groups", columnGroups.size());
        
        return columnGroups;
    }
    
    /**
     * Converts detected column groups to quasi-identifier group entities.
     */
    private List<CorrelatedQuasiIdentifierGroup> convertToQuasiIdentifierGroups(
            List<Set<ColumnInfo>> columnGroups,
            Map<ColumnInfo, SampleData> columnDataMap) {
        
        List<CorrelatedQuasiIdentifierGroup> result = new ArrayList<>();
        
        int groupNumber = 1;
        for (Set<ColumnInfo> columnGroup : columnGroups) {
            // Create a group with basic properties
            CorrelatedQuasiIdentifierGroup group = new CorrelatedQuasiIdentifierGroup();
            group.setGroupName("QI_GROUP_" + groupNumber++);
            group.setCreatedAt(LocalDateTime.now());
            
            // Set clustering method directly
            String clusteringMethodValue = config.isUseMachineLearning() ? "ML_CLUSTERING" : "GRAPH_CORRELATION";
            group.setClusteringMethod(clusteringMethodValue);
            
            // Calculate distinct combinations
            int estimatedDistinctCombinations = estimateDistinctCombinations(columnGroup, columnDataMap);
            group.setDistinctCombinations(estimatedDistinctCombinations);
            
            // Add columns to group
            for (ColumnInfo column : columnGroup) {
                SampleData data = columnDataMap.get(column);
                ColumnValueDistributionAnalyzer.ColumnDistributionMetrics metrics = 
                        distributionAnalyzer.calculateDistribution(column, data);
                
                // Calculate contribution score
                double contributionScore = calculateContributionScore(column, columnGroup, columnDataMap);
                
                group.addColumn(
                    column,
                    contributionScore,
                    metrics.getDistinctValueCount(),
                    metrics.getEntropy()
                );
            }
            
            // Calculate re-identification risk score and set it
            double riskScore = calculateReIdentificationRisk(group);
            group.setReIdentificationRiskScore(riskScore);
            
            result.add(group);
            
            logger.debug("Created quasi-identifier group: {} with {} columns, risk score: {}, method: {}", 
                    group.getGroupName(), group.getColumns().size(), riskScore, clusteringMethodValue);
        }
        
        return result;
    }
    
    /**
     * Estimates number of distinct combinations in a column group.
     * This is a simplified implementation.
     */
    private int estimateDistinctCombinations(Set<ColumnInfo> columns, Map<ColumnInfo, SampleData> columnDataMap) {
        // Simple estimation based on each column's distinct values
        double combinationEstimate = 1.0;
        for (ColumnInfo column : columns) {
            SampleData data = columnDataMap.get(column);
            ColumnValueDistributionAnalyzer.ColumnDistributionMetrics metrics = 
                    distributionAnalyzer.calculateDistribution(column, data);
                    
            // Use adjustment factor to account for correlation between columns
            double adjustmentFactor = 0.7; // Arbitrary value for now, would be based on correlation in real impl
            combinationEstimate *= Math.max(1, metrics.getDistinctValueCount() * adjustmentFactor);
        }
        
        return (int) Math.min(Integer.MAX_VALUE, combinationEstimate);
    }
    
    /**
     * Calculates the contribution score of a column to its quasi-identifier group.
     */
    private double calculateContributionScore(ColumnInfo column, Set<ColumnInfo> columnGroup, 
                                            Map<ColumnInfo, SampleData> columnDataMap) {
        // Simple calculation based on column's distinctness relative to the group
        SampleData data = columnDataMap.get(column);
        ColumnValueDistributionAnalyzer.ColumnDistributionMetrics metrics = 
                distributionAnalyzer.calculateDistribution(column, data);
                
        double entropy = metrics.getEntropy();
        double maxEntropy = Math.log(metrics.getTotalSampleCount()) / Math.log(2);
        
        // Normalize entropy to 0-1 scale
        double normalizedEntropy = maxEntropy > 0 ? entropy / maxEntropy : 0;
        
        // Calculate average correlation with other columns in group
        double totalCorrelation = 0.0;
        int correlationCount = 0;
        
        for (ColumnInfo otherColumn : columnGroup) {
            if (!otherColumn.equals(column)) {
                ColumnCorrelationCalculator.ColumnPair pair = 
                        new ColumnCorrelationCalculator.ColumnPair(column, otherColumn);
                
                Map<ColumnCorrelationCalculator.ColumnPair, Double> correlationMatrix = 
                        correlationCalculator.calculateCorrelationMatrix(
                            Map.of(column, columnDataMap.get(column), 
                                   otherColumn, columnDataMap.get(otherColumn))
                        );
                
                double correlation = correlationMatrix.getOrDefault(pair, 0.0);
                totalCorrelation += correlation;
                correlationCount++;
            }
        }
        
        double avgCorrelation = correlationCount > 0 ? totalCorrelation / correlationCount : 0;
        
        // Contribution is based on entropy and correlation
        return 0.7 * normalizedEntropy + 0.3 * avgCorrelation;
    }
    
    /**
     * Calculates the re-identification risk for a quasi-identifier group.
     */
    private double calculateReIdentificationRisk(CorrelatedQuasiIdentifierGroup group) {
        // Base risk calculation on uniqueness and distinctness
        double kAnonymity = group.getKAnonymityValue();
        
        // Normalize k-anonymity to a 0-1 risk score (higher k-anonymity = lower risk)
        double kAnonymityFactor = Math.clamp(config.getKAnonymityThreshold() / (kAnonymity + 1), 0, 1);
        
        // Consider entropy contribution
        double avgEntropy = group.getColumns().stream()
                .mapToDouble(mapping -> mapping.getDistributionEntropy())
                .average()
                .orElse(0);
        
        // Calculate max possible entropy based on column cardinalities
        double maxPossibleEntropy = Math.log(
            group.getColumns().stream()
                .mapToDouble(mapping -> mapping.getCardinality())
                .average()
                .orElse(1)
        ) / Math.log(2);
        
        double normalizedEntropy = maxPossibleEntropy > 0 ? avgEntropy / maxPossibleEntropy : 0;
        
        // Risk formula combining uniqueness and informativeness
        double risk = 0.6 * kAnonymityFactor + 0.4 * normalizedEntropy;
        
        return Math.clamp(risk, 0, 1); // Ensure risk is between 0 and 1
    }
    
    /**
     * Point class for DBSCAN clustering based on correlation distances.
     */
    private static class ColumnClusterPoint implements Clusterable {
        private final ColumnInfo column;
        private final Map<ColumnInfo, Map<ColumnInfo, Double>> distanceMap;
        
        public ColumnClusterPoint(ColumnInfo column, Map<ColumnInfo, Map<ColumnInfo, Double>> distanceMap) {
            this.column = column;
            this.distanceMap = distanceMap;
        }
        
        public ColumnInfo getColumn() {
            return column;
        }
        
        @Override
        public double[] getPoint() {
            // For DBSCAN, we only need to implement distance function
            // This is a placeholder to satisfy the interface
            return new double[] { 0.0 };
        }
        
        // The distanceFrom method is used by the DBSCAN implementation
        // It may report as "unused" because it's called via the interface
        @SuppressWarnings("unused") // Required by DBSCANClusterer through the Clusterable interface
        public double distanceFrom(Clusterable other) {
            if (!(other instanceof ColumnClusterPoint)) {
                throw new IllegalArgumentException("Can only calculate distance to other ColumnClusterPoint");
            }
            
            ColumnClusterPoint otherPoint = (ColumnClusterPoint) other;
            return distanceMap.get(this.column).get(otherPoint.column);
        }
    }
}