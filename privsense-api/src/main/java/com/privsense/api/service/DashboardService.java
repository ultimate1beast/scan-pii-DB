package com.privsense.api.service;

import com.privsense.api.dto.DashboardSummaryDTO;
import com.privsense.api.dto.PagedResultDTO;
import com.privsense.api.dto.PiiSummaryDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanTrendsDTO;

import java.util.List;

/**
 * Service interface for dashboard operations.
 */
public interface DashboardService {

    /**
     * Get summary statistics for the dashboard.
     *
     * @return A DTO containing summary statistics
     */
    DashboardSummaryDTO getDashboardSummary();

    /**
     * Get trend data for chart visualizations.
     *
     * @param timeframe The time period to get trends for (e.g., "7", "30", "90" for days)
     * @return A DTO containing trend data
     */
    ScanTrendsDTO getScanTrends(String timeframe);

    /**
     * Get most recent scan jobs with pagination, sorting and filtering.
     *
     * @param page The page number (0-based)
     * @param size The size of each page
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction ("asc" or "desc")
     * @return A paginated list of scan job responses
     */
    PagedResultDTO<ScanJobResponse> getRecentScans(int page, int size, String sortBy, String sortDirection);

    /**
     * Get most recent scan jobs.
     *
     * @param limit The maximum number of scan jobs to return
     * @return A list of scan job responses
     */
    List<ScanJobResponse> getRecentScans(int limit);

    /**
     * Get the most common PII types found.
     *
     * @param limit The maximum number of PII types to return
     * @return A list of PII summary DTOs
     */
    List<PiiSummaryDTO> getTopPiiFindings(int limit);
}