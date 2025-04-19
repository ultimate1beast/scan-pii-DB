package com.privsense.sampler.util;

import com.privsense.core.model.SampleData;
import org.springframework.stereotype.Component;



/**
 * Utility class for calculating Shannon entropy of sampled data.
 */
@Component
public class EntropyCalculator {

    /**
     * Calculates the Shannon entropy of the sample data.
     * H(X) = - sum(p(x_i) * log2(p(x_i)))
     * 
     * @param sampleData The sample data to calculate entropy for
     * @return The calculated entropy value
     */
    public double calculateEntropy(SampleData sampleData) {
        // Use the calculateEntropy method already implemented in SampleData
        return sampleData.calculateEntropy();
    }
}