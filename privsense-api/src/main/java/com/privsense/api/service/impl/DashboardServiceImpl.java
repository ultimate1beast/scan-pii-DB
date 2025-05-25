package com.privsense.api.service.impl;

import com.privsense.api.dto.DashboardSummaryDTO;
import com.privsense.api.dto.PagedResultDTO;
import com.privsense.api.dto.PiiSummaryDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanTrendsDTO;
import com.privsense.api.dto.ScanTrendsDTO.TimeSeriesDataPoint;
import com.privsense.api.exception.DashboardException;
import com.privsense.api.exception.InvalidParameterException;
import com.privsense.api.service.DashboardService;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.ScanMetadata;
import com.privsense.core.service.ScanPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the DashboardService interface.
 * Provides methods to gather and process data for the dashboard.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ScanPersistenceService scanPersistenceService;
    private static final int MAX_SUMMARY_CACHE_DURATION_MINUTES = 15;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    
    // Cache for dashboard summary with timestamp to maintain freshness
    private DashboardSummaryDTO cachedSummary;
    private Instant cacheSummaryTimestamp;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        try {
            // Check if we have a recent cache that can be returned
            if (isSummaryCacheValid()) {
                log.debug("Returning cached dashboard summary");
                return cachedSummary;
            }
            
            log.debug("Generating new dashboard summary");
            List<ScanMetadata> allScans = scanPersistenceService.getAllScans();
            
            // Calculate summary statistics
            long totalScans = allScans.size();
            long completedScans = allScans.stream()
                    .filter(scan -> scan.getStatus() == ScanMetadata.ScanStatus.COMPLETED)
                    .count();
            long failedScans = allScans.stream()
                    .filter(scan -> scan.getStatus() == ScanMetadata.ScanStatus.FAILED)
                    .count();
            
            // Calculate PII statistics
            long totalColumnsScanned = allScans.stream()
                    .filter(scan -> scan.getTotalColumnsScanned() != null)
                    .mapToLong(ScanMetadata::getTotalColumnsScanned)
                    .sum();
            
            long totalPiiFound = allScans.stream()
                    .filter(scan -> scan.getTotalPiiColumnsFound() != null)
                    .mapToLong(ScanMetadata::getTotalPiiColumnsFound)
                    .sum();
            
            double piiPercentage = totalColumnsScanned > 0 
                    ? (double) totalPiiFound / totalColumnsScanned * 100 
                    : 0;
            
            // Build the summary
            DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                    .totalScans(totalScans)
                    .completedScans(completedScans)
                    .failedScans(failedScans)
                    .totalColumnsScanned(totalColumnsScanned)
                    .totalPiiColumnsFound(totalPiiFound)
                    .piiPercentage(Math.round(piiPercentage * 100.0) / 100.0)  // Round to 2 decimal places
                    .lastUpdated(Instant.now())
                    .build();
                    
            // Update the cache
            this.cachedSummary = summary;
            this.cacheSummaryTimestamp = Instant.now();
            
            return summary;
        } catch (Exception e) {
            log.error("Error generating dashboard summary", e);
            throw new DashboardException("Failed to generate dashboard summary", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ScanTrendsDTO getScanTrends(String timeframe) {
        try {
            // Validate and parse timeframe
            if (timeframe == null || timeframe.trim().isEmpty()) {
                timeframe = "30"; // Default to 30 days
            }
            
            // Parse timeframe and get appropriate data range
            Instant startTime;
            Instant endTime = Instant.now();
            
            switch (timeframe.toUpperCase()) {
                case "LAST_DAY":
                case "1":
                    startTime = endTime.minus(1, ChronoUnit.DAYS);
                    break;
                case "LAST_WEEK":
                case "7":
                    startTime = endTime.minus(7, ChronoUnit.DAYS);
                    break;
                case "LAST_MONTH":
                case "30":
                    startTime = endTime.minus(30, ChronoUnit.DAYS);
                    break;
                case "LAST_YEAR":
                case "365":
                    startTime = endTime.minus(365, ChronoUnit.DAYS);
                    break;
                default:
                    // Try to parse as a number of days
                    try {
                        int days = Integer.parseInt(timeframe);
                        if (days <= 0) {
                            throw new InvalidParameterException("Timeframe must be a positive number of days");
                        } else if (days > 3650) { // Reasonable limit of 10 years
                            throw new InvalidParameterException("Timeframe cannot exceed 3650 days (10 years)");
                        }
                        startTime = endTime.minus(days, ChronoUnit.DAYS);
                    } catch (NumberFormatException e) {
                        // Try to parse as an ISO date
                        try {
                            startTime = Instant.parse(timeframe);
                            if (startTime.isAfter(endTime)) {
                                throw new InvalidParameterException("Start date cannot be in the future");
                            }
                        } catch (DateTimeParseException ex) {
                            log.warn("Invalid timeframe '{}', defaulting to LAST_MONTH (30 days)", timeframe);
                            startTime = endTime.minus(30, ChronoUnit.DAYS);
                        }
                    }
            }
            
            // Get scans within timeframe
            List<ScanMetadata> scansInTimeframe = scanPersistenceService.getScansByTimeRange(startTime, endTime);
            
            // Group by date and count
            Map<LocalDate, Long> scanCountByDate = scansInTimeframe.stream()
                    .map(scan -> LocalDate.ofInstant(scan.getStartTime(), ZoneId.systemDefault()))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            
            // Group PII findings by date
            Map<LocalDate, Long> piiByDate = new HashMap<>();
            for (ScanMetadata scan : scansInTimeframe) {
                if (scan.getTotalPiiColumnsFound() != null && scan.getTotalPiiColumnsFound() > 0) {
                    LocalDate scanDate = LocalDate.ofInstant(scan.getStartTime(), ZoneId.systemDefault());
                    // Convert Integer to Long before merging
                    Long piiColumnsFound = Long.valueOf(scan.getTotalPiiColumnsFound());
                    piiByDate.merge(scanDate, piiColumnsFound, Long::sum);
                }
            }
            
            // Fill in missing dates to ensure continuous data for charts
            fillMissingDates(startTime, endTime, scanCountByDate, piiByDate);
            
            // Create time series data points for charting
            List<TimeSeriesDataPoint> timeSeriesData = new ArrayList<>();
            Set<LocalDate> allDates = new HashSet<>();
            allDates.addAll(scanCountByDate.keySet());
            allDates.addAll(piiByDate.keySet());
            
            for (LocalDate date : allDates.stream().sorted().collect(Collectors.toList())) {
                long scanCount = scanCountByDate.getOrDefault(date, 0L);
                long piiCount = piiByDate.getOrDefault(date, 0L);
                timeSeriesData.add(new TimeSeriesDataPoint(date, scanCount, piiCount));
            }
            
            return ScanTrendsDTO.builder()
                    .timeframe(timeframe)
                    .startDate(startTime.toString())
                    .endDate(endTime.toString())
                    .scanCountByDate(scanCountByDate)
                    .piiCountByDate(piiByDate)
                    .timeSeriesData(timeSeriesData)
                    .build();
                    
        } catch (InvalidParameterException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating scan trends data", e);
            throw new DashboardException("Failed to generate scan trends data", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResultDTO<ScanJobResponse> getRecentScans(int page, int size, String sortBy, String sortDirection) {
        try {
            // Input validation
            if (page < 0) {
                throw new InvalidParameterException("Page number must be non-negative");
            }
            
            if (size <= 0) {
                size = DEFAULT_PAGE_SIZE;
            } else if (size > MAX_PAGE_SIZE) {
                size = MAX_PAGE_SIZE;
            }
            
            // Default sort by start time, newest first if not specified
            if (sortBy == null || sortBy.trim().isEmpty()) {
                sortBy = "startTime";
            }
            
            // Note: sortBy and sortDirection are validated but not used directly since 
            // the underlying getPagedScans method already applies sorting by startTime in descending order
            // This is kept for API consistency and potential future implementation
            
            // Get paged scans using the correct method signature
            List<ScanMetadata> scanList = scanPersistenceService.getPagedScans(page, size);
            long totalElements = scanPersistenceService.countAllScans();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // Convert to DTOs
            List<ScanJobResponse> responseList = scanList.stream()
                    .map(this::convertToScanJobResponse)
                    .collect(Collectors.toList());
            
            // Create paged result
            return new PagedResultDTO<>(
                    responseList,
                    page,
                    size,
                    totalElements,
                    totalPages,
                    page == 0,
                    page >= totalPages - 1 || totalPages == 0
            );
            
        } catch (InvalidParameterException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving recent scans", e);
            throw new DashboardException("Failed to retrieve recent scans", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanJobResponse> getRecentScans(int limit) {
        try {
            // Input validation
            if (limit <= 0) {
                throw new InvalidParameterException("Limit must be positive");
            }
            if (limit > MAX_PAGE_SIZE) {
                limit = MAX_PAGE_SIZE;
            }
            
            // Get recent scans
            List<ScanMetadata> recentScans = scanPersistenceService.getRecentScans(limit);
            
            // Convert to DTOs
            return recentScans.stream()
                    .map(this::convertToScanJobResponse)
                    .collect(Collectors.toList());
                    
        } catch (InvalidParameterException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving recent scans with limit {}", limit, e);
            throw new DashboardException("Failed to retrieve recent scans", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PiiSummaryDTO> getTopPiiFindings(int limit) {
        try {
            // Input validation
            if (limit <= 0) {
                throw new InvalidParameterException("Limit must be positive");
            }
            if (limit > 100) { // Reasonable limit for "top" findings
                limit = 100;
            }
            
            // Get all scan IDs for completed scans
            List<UUID> completedScanIds = scanPersistenceService.getScansByStatus(ScanMetadata.ScanStatus.COMPLETED)
                    .stream()
                    .map(ScanMetadata::getId)
                    .collect(Collectors.toList());
            
            if (completedScanIds.isEmpty()) {
                log.debug("No completed scans found for PII findings");
                return Collections.emptyList();
            }
            
            // Get all PII findings - process in batches to avoid memory issues
            Map<String, Integer> piiTypeCount = new HashMap<>();
            int batchSize = 10; // Process 10 scans at a time
            
            for (int i = 0; i < completedScanIds.size(); i += batchSize) {
                int toIndex = Math.min(i + batchSize, completedScanIds.size());
                List<UUID> batchIds = completedScanIds.subList(i, toIndex);
                
                for (UUID scanId : batchIds) {
                    List<DetectionResult> piiResults = scanPersistenceService.getPiiResultsByScanId(scanId);
                    
                    for (DetectionResult result : piiResults) {
                        if (result.getPiiType() != null && !result.getPiiType().isEmpty()) {
                            piiTypeCount.merge(result.getPiiType(), 1, Integer::sum);
                        }
                    }
                }
            }
            
            // Sort by count and limit results
            return piiTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .map(entry -> PiiSummaryDTO.builder()
                            .piiType(entry.getKey())
                            .count(entry.getValue())
                            .percentage(calculatePercentage(entry.getValue(), piiTypeCount.values().stream().mapToInt(Integer::intValue).sum()))
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (InvalidParameterException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving top PII findings", e);
            throw new DashboardException("Failed to retrieve top PII findings", e);
        }
    }
    
    /**
     * Helper method to convert ScanMetadata to ScanJobResponse
     */
    private ScanJobResponse convertToScanJobResponse(ScanMetadata scan) {
        ScanJobResponse response = new ScanJobResponse();
        response.setJobId(scan.getId());
        response.setConnectionId(scan.getConnectionId());
        response.setStatus(scan.getStatus().name());
        
        // Set database information if available
        response.setDatabaseName(scan.getDatabaseName());
        response.setDatabaseProductName(scan.getDatabaseProductName());
        
        // Convert times
        if (scan.getStartTime() != null) {
            response.setStartTime(scan.getStartTime().toString());
        }
        
        if (scan.getEndTime() != null) {
            response.setEndTime(scan.getEndTime().toString());
        }
        
        // Set PII scan results if available
        if (scan.getTotalColumnsScanned() != null) {
            response.setTotalColumnsScanned(scan.getTotalColumnsScanned());
        }
        if (scan.getTotalPiiColumnsFound() != null) {
            response.setTotalPiiColumnsFound(scan.getTotalPiiColumnsFound());
        }
        
        // Set completion flags based on status
        response.setCompleted(scan.getStatus() == ScanMetadata.ScanStatus.COMPLETED);
        response.setFailed(scan.getStatus() == ScanMetadata.ScanStatus.FAILED || 
                scan.getStatus() == ScanMetadata.ScanStatus.CANCELLED);
        
        // Set error message if available
        response.setErrorMessage(scan.getErrorMessage());
        
        return response;
    }
    
    /**
     * Helper method to check if the cached summary is still valid
     */
    private boolean isSummaryCacheValid() {
        if (cachedSummary == null || cacheSummaryTimestamp == null) {
            return false;
        }
        
        // Check if cache has expired
        Instant cacheExpiryTime = cacheSummaryTimestamp.plus(MAX_SUMMARY_CACHE_DURATION_MINUTES, ChronoUnit.MINUTES);
        return Instant.now().isBefore(cacheExpiryTime);
    }
    
    /**
     * Helper method to calculate percentage
     */
    private double calculatePercentage(int value, int total) {
        if (total == 0) return 0;
        double percentage = ((double) value / total) * 100.0;
        // Round to 2 decimal places
        return Math.round(percentage * 100.0) / 100.0;
    }
    
    /**
     * Helper method to fill in missing dates in the time series data
     */
    private void fillMissingDates(Instant startTime, Instant endTime, 
                                 Map<LocalDate, Long> scanCountByDate,
                                 Map<LocalDate, Long> piiByDate) {
                                 
        LocalDate startDate = LocalDate.ofInstant(startTime, ZoneId.systemDefault());
        LocalDate endDate = LocalDate.ofInstant(endTime, ZoneId.systemDefault());
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            // Ensure this date exists in both maps
            scanCountByDate.putIfAbsent(currentDate, 0L);
            piiByDate.putIfAbsent(currentDate, 0L);
            
            currentDate = currentDate.plusDays(1);
        }
    }
}