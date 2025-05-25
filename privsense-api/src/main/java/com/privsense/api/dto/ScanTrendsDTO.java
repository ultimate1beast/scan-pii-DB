package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for scan trend data for visualizations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ScanTrendsDTO extends BaseResponseDTO {
    
    /**
     * The requested timeframe (e.g., "LAST_DAY", "LAST_WEEK", "LAST_MONTH", "LAST_YEAR")
     */
    private String timeframe;
    
    /**
     * The start date of the trends period in ISO format
     */
    private String startDate;
    
    /**
     * The end date of the trends period in ISO format
     */
    private String endDate;
    
    /**
     * Number of scans performed each day within the timeframe
     */
    private Map<LocalDate, Long> scanCountByDate;
    
    /**
     * Number of PII columns found each day within the timeframe
     */
    private Map<LocalDate, Long> piiCountByDate;
    
    /**
     * Time series data points optimized for chart visualization
     */
    private List<TimeSeriesDataPoint> timeSeriesData;
    
    /**
     * Inner class for time series data points to be used in charts.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        /**
         * The date of this data point
         */
        private LocalDate date;
        
        /**
         * Number of scans performed on this date
         */
        private long scanCount;
        
        /**
         * Number of PII columns found on this date
         */
        private long piiCount;
    }
}