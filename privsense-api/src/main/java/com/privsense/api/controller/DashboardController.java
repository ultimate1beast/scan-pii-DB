package com.privsense.api.controller;

import com.privsense.api.dto.DashboardSummaryDTO;
import com.privsense.api.dto.PiiSummaryDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanTrendsDTO;
import com.privsense.api.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for dashboard-related operations.
 * Provides endpoints to retrieve metrics and visualizations for the dashboard.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs for retrieving dashboard data and metrics")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;

    /**
     * Returns summary statistics for the dashboard
     */
    @GetMapping("/summary")
    @Operation(
        summary = "Get dashboard summary",
        description = "Returns summary statistics including counts of connections, scans, and PII findings"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Summary retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardSummaryDTO.class))
    )
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        logger.info("Retrieving dashboard summary");
        DashboardSummaryDTO summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Returns trend data for visualizations on the dashboard
     */
    @GetMapping("/trends")
    @Operation(
        summary = "Get scan trends data",
        description = "Returns time series data for chart visualizations"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Trends data retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanTrendsDTO.class))
    )
    public ResponseEntity<ScanTrendsDTO> getScanTrends(
            @RequestParam(required = false, defaultValue = "30") String timeframe) {
        logger.info("Retrieving scan trends for timeframe: {}", timeframe);
        ScanTrendsDTO trends = dashboardService.getScanTrends(timeframe);
        return ResponseEntity.ok(trends);
    }

    /**
     * Returns most recent scan jobs
     */
    @GetMapping("/recent-scans")
    @Operation(
        summary = "Get recent scans",
        description = "Returns the most recent scan jobs"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Recent scans retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanJobResponse.class))
    )
    public ResponseEntity<List<ScanJobResponse>> getRecentScans(
            @RequestParam(defaultValue = "5") int limit) {
        logger.info("Retrieving {} most recent scans", limit);
        List<ScanJobResponse> recentScans = dashboardService.getRecentScans(limit);
        return ResponseEntity.ok(recentScans);
    }

    /**
     * Returns most common PII types found
     */
    @GetMapping("/top-findings")
    @Operation(
        summary = "Get top PII findings",
        description = "Returns the most common PII types found"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Top findings retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = PiiSummaryDTO.class))
    )
    public ResponseEntity<List<PiiSummaryDTO>> getTopPiiFindings(
            @RequestParam(defaultValue = "5") int limit) {
        logger.info("Retrieving top {} PII findings", limit);
        List<PiiSummaryDTO> topFindings = dashboardService.getTopPiiFindings(limit);
        return ResponseEntity.ok(topFindings);
    }
}