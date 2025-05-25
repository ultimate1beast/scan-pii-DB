package com.privsense.api.model;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Data structure for storing API usage metrics that can be persisted.
 * Implements Serializable to allow saving to disk.
 */
@Data
public class PersistentMetricsData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Request metrics
    private int totalRequests = 0;
    private Map<String, Integer> requestsByEndpoint = new HashMap<>();
    
    // Error metrics
    private int totalErrors = 0;
    private Map<Integer, Integer> errorsByStatus = new HashMap<>();
    
    // Time information
    private long lastPersistTime = System.currentTimeMillis();
}