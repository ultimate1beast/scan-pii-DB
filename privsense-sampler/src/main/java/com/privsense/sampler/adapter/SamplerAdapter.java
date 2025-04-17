package com.privsense.sampler.adapter;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SamplingConfig;
import com.privsense.core.service.DataSampler;
import com.privsense.core.service.Sampler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Adapter class that implements the Sampler interface by delegating to DataSampler.
 * This provides compatibility between the core Sampler interface and the actual DataSampler implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SamplerAdapter implements Sampler {

    private final DataSampler dataSampler;

    @Override
    public Map<ColumnInfo, SampleData> extractSamples(Connection connection, List<ColumnInfo> columns, SamplingConfig config) {
        log.debug("Delegating sample extraction for {} columns with sample size {}", 
                columns.size(), config.getSampleSize());
        
        // Use the DataSampler implementation to sample the columns directly
        // rather than going through the schema
        List<SampleData> sampledData = dataSampler.sampleColumns(
                connection,
                columns,
                config.getSampleSize()
        );
        
        // Convert the list of SampleData to the required Map<ColumnInfo, SampleData>
        Map<ColumnInfo, SampleData> sampleMap = new java.util.HashMap<>();
        for (SampleData sample : sampledData) {
            sampleMap.put(sample.getColumnInfo(), sample);
        }
        
        return sampleMap;
    }
}