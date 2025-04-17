package com.privsense.sampler.factory;

import com.privsense.core.exception.DataSamplingException;
import com.privsense.sampler.strategy.DbSpecificSamplingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for providing the appropriate database-specific sampling strategy.
 */
@Component
public class SamplingStrategyFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(SamplingStrategyFactory.class);
    
    private final Map<String, DbSpecificSamplingStrategy> strategies;
    
    /**
     * Creates a new factory with all the available sampling strategies.
     * 
     * @param availableStrategies List of all DbSpecificSamplingStrategy beans
     */
    public SamplingStrategyFactory(List<DbSpecificSamplingStrategy> availableStrategies) {
        strategies = availableStrategies.stream()
            .collect(Collectors.toMap(
                DbSpecificSamplingStrategy::getSupportedDatabaseName, 
                Function.identity()
            ));
        
        logger.info("Initialized with {} database sampling strategies: {}", 
            strategies.size(), strategies.keySet());
    }
    
    /**
     * Gets the appropriate sampling strategy for the given database product name.
     * 
     * @param databaseProductName The database product name from DatabaseMetaData
     * @return The appropriate sampling strategy
     * @throws DataSamplingException if no strategy is available for the database
     */
    public DbSpecificSamplingStrategy getStrategy(String databaseProductName) {
        // Try exact match first
        DbSpecificSamplingStrategy strategy = strategies.get(databaseProductName);
        
        // If no exact match, try partial match (e.g. "PostgreSQL" vs "PostgreSQL 12.1")
        if (strategy == null) {
            for (Map.Entry<String, DbSpecificSamplingStrategy> entry : strategies.entrySet()) {
                if (databaseProductName.contains(entry.getKey())) {
                    strategy = entry.getValue();
                    break;
                }
            }
        }
        
        if (strategy == null) {
            throw new DataSamplingException("No sampling strategy available for database: " + databaseProductName);
        }
        
        logger.debug("Selected strategy {} for database: {}", 
            strategy.getClass().getSimpleName(), databaseProductName);
        
        return strategy;
    }
}