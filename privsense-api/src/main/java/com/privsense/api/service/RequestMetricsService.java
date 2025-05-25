package com.privsense.api.service;

import com.privsense.api.model.PersistentMetricsData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for tracking API request metrics.
 * Maintains counts of requests by endpoint and error counts.
 * Supports persistence to survive application restarts.
 */
@Service
@Slf4j
public class RequestMetricsService {

    @Value("${metrics.persistence.file:./metrics-data.ser}")
    private String persistenceFilePath;
    
    @Value("${metrics.persistence.enabled:true}")
    private boolean persistenceEnabled;
    
    @Value("${metrics.persistence.interval:300000}")
    private long persistenceIntervalMs; // Default: 5 minutes

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final Map<String, AtomicInteger> requestsByEndpoint = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicInteger> errorsByStatus = new ConcurrentHashMap<>();
    private final AtomicInteger totalErrors = new AtomicInteger(0);
    
    // Track requests within the last minute
    private final Map<Instant, AtomicInteger> requestTimestamps = new ConcurrentHashMap<>();
    
    /**
     * Initializes the service, loading persistent data if available.
     */
    @PostConstruct
    public void init() {
        if (persistenceEnabled) {
            loadPersistedData();
        }
    }
    
    /**
     * Persists metrics data on application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        if (persistenceEnabled) {
            persistData();
        }
    }
    
    /**
     * Scheduled task to periodically persist metrics data.
     */
    @Scheduled(fixedDelayString = "${metrics.persistence.interval:300000}")
    public void scheduledPersistence() {
        if (persistenceEnabled) {
            persistData();
        }
    }
    
    /**
     * Persists the current metrics to disk.
     */
    public synchronized void persistData() {
        try {
            File file = new File(persistenceFilePath);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
            }
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                PersistentMetricsData data = new PersistentMetricsData();
                
                // Set request data
                data.setTotalRequests(totalRequests.get());
                
                // Copy request by endpoint counts
                Map<String, Integer> endpointMap = new ConcurrentHashMap<>();
                requestsByEndpoint.forEach((endpoint, count) -> endpointMap.put(endpoint, count.get()));
                data.setRequestsByEndpoint(endpointMap);
                
                // Set error data
                data.setTotalErrors(totalErrors.get());
                
                // Copy error by status counts
                Map<Integer, Integer> errorMap = new ConcurrentHashMap<>();
                errorsByStatus.forEach((status, count) -> errorMap.put(status, count.get()));
                data.setErrorsByStatus(errorMap);
                
                data.setLastPersistTime(System.currentTimeMillis());
                
                // Write the data
                oos.writeObject(data);
                log.debug("Metrics data persisted to {}", persistenceFilePath);
            }
        } catch (IOException e) {
            log.error("Failed to persist metrics data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Loads previously persisted metrics data.
     */
    public synchronized void loadPersistedData() {
        File file = new File(persistenceFilePath);
        if (!file.exists() || !file.canRead()) {
            log.debug("No persisted metrics data found at {}", persistenceFilePath);
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            PersistentMetricsData data = (PersistentMetricsData) ois.readObject();
            
            // Set request counters
            totalRequests.set(data.getTotalRequests());
            
            // Set requests by endpoint
            data.getRequestsByEndpoint().forEach((endpoint, count) -> {
                requestsByEndpoint.put(endpoint, new AtomicInteger(count));
            });
            
            // Set error counters
            totalErrors.set(data.getTotalErrors());
            
            // Set errors by status
            data.getErrorsByStatus().forEach((status, count) -> {
                errorsByStatus.put(status, new AtomicInteger(count));
            });
            
            log.info("Loaded persisted metrics data from {}: {} total requests, {} errors", 
                    persistenceFilePath, data.getTotalRequests(), data.getTotalErrors());
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to load persisted metrics data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Resets all metrics counters.
     */
    public synchronized void resetMetrics() {
        totalRequests.set(0);
        requestsByEndpoint.clear();
        totalErrors.set(0);
        errorsByStatus.clear();
        requestTimestamps.clear();
        log.info("All metrics counters have been reset");
        
        // Persist the reset state if persistence is enabled
        if (persistenceEnabled) {
            persistData();
        }
    }
    
    /**
     * Records a new API request.
     *
     * @param endpoint The endpoint being called
     */
    public void recordRequest(String endpoint) {
        totalRequests.incrementAndGet();
        requestsByEndpoint.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Record timestamp for last-minute tracking
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        requestTimestamps.computeIfAbsent(now, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Clean up old timestamps (older than 1 minute)
        Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
        requestTimestamps.keySet().removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));
    }
    
    /**
     * Records an error response.
     *
     * @param statusCode The HTTP status code of the error
     */
    public void recordError(int statusCode) {
        totalErrors.incrementAndGet();
        errorsByStatus.computeIfAbsent(statusCode, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Gets the total number of recorded requests.
     *
     * @return The total request count
     */
    public int getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * Gets the count of requests in the last minute.
     *
     * @return The count of requests in the last minute
     */
    public int getRequestsLastMinute() {
        return requestTimestamps.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
    }
    
    /**
     * Gets the counts of requests by endpoint.
     *
     * @return A map of endpoints to request counts
     */
    public Map<String, Integer> getRequestsByEndpoint() {
        Map<String, Integer> result = new ConcurrentHashMap<>();
        requestsByEndpoint.forEach((endpoint, count) -> result.put(endpoint, count.get()));
        return result;
    }
    
    /**
     * Gets the total number of recorded errors.
     *
     * @return The total error count
     */
    public int getTotalErrors() {
        return totalErrors.get();
    }
    
    /**
     * Gets the error rate as a percentage of total requests.
     *
     * @return The error rate as a string percentage
     */
    public String getErrorRate() {
        if (totalRequests.get() == 0) {
            return "0.0%";
        }
        double rate = (double) totalErrors.get() / totalRequests.get() * 100;
        return String.format("%.1f%%", rate);
    }
    
    /**
     * Gets the counts of errors by status code.
     *
     * @return A map of status codes to error counts
     */
    public Map<Integer, Integer> getErrorsByStatus() {
        Map<Integer, Integer> result = new ConcurrentHashMap<>();
        errorsByStatus.forEach((status, count) -> result.put(status, count.get()));
        return result;
    }
}