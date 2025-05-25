package com.privsense.api.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO for scan trend data displayed on the dashboard.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanTrendsDTO extends BaseResponseDTO {

    /**
     * The timeframe for which trends are calculated (e.g., LAST_DAY, LAST_WEEK, LAST_MONTH)
     */
    private String timeframe;
    
    /**
     * Count of scans by date
     */
    private Map<LocalDate, Long> scanCountByDate;
    
    /**
     * Count of PII findings by date
     */
    private Map<LocalDate, Long> piiCountByDate;
    
    /**
     * List of time series data points for chart rendering
     */
    private List<TimeSeriesDataPoint> timeSeriesData;
    
    /**
     * Inner class representing a data point in a time series chart
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        /**
         * Date of the data point
         */
        private LocalDate date;
        
        /**
         * Number of scans on this date
         */
        private long scanCount;
        
        /**
         * Number of PII findings on this date
         */
        private long piiCount;
    }
}