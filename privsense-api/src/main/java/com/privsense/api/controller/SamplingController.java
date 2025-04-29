package com.privsense.api.controller;

import com.privsense.api.dto.BatchSamplingRequest;
import com.privsense.api.dto.BatchSamplingResponse;
import com.privsense.api.dto.SamplingRequest;
import com.privsense.api.dto.SamplingResponse;
import com.privsense.api.dto.config.SamplingConfigDTO;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.service.SamplingService;
import com.privsense.core.exception.DataSamplingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for database sampling operations.
 * Provides endpoints to test sampling functionality on database columns.
 */
@RestController
@RequestMapping("/api/v1/sampling")
@RequiredArgsConstructor
@Tag(name = "Database Sampling", description = "APIs for testing database column sampling")
public class SamplingController {

    private final SamplingService samplingService;

    /**
     * Performs a test sampling operation on a database column.
     */
    @PostMapping
    @Operation(
        summary = "Test column sampling",
        description = "Samples data from a specific column to validate sampling functionality"
    )
    @ApiResponse(responseCode = "200", description = "Sampling successful")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "404", description = "Database connection or column not found")
    public ResponseEntity<SamplingResponse> testSampling(@Valid @RequestBody SamplingRequest request) {
        try {
            SamplingResponse response = samplingService.performSampling(request);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataSamplingException e) {
            throw new RuntimeException("Sampling failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Performs parallel sampling across multiple tables to test parallel sampler performance.
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Test parallel sampling across multiple tables",
        description = "Samples data from multiple tables and columns in parallel to validate the performance of parallel sampling"
    )
    @ApiResponse(responseCode = "200", description = "Batch sampling completed")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "404", description = "Database connection not found")
    public ResponseEntity<BatchSamplingResponse> testBatchSampling(@Valid @RequestBody BatchSamplingRequest request) {
        try {
            BatchSamplingResponse response = samplingService.performBatchSampling(request);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Batch sampling failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieves sampling configuration options.
     */
    @GetMapping("/configuration")
    @Operation(
        summary = "Get sampling configuration",
        description = "Returns the available sampling configuration options"
    )
    public ResponseEntity<SamplingConfigDTO> getSamplingConfiguration() {
        return ResponseEntity.ok(samplingService.getSamplingConfiguration());
    }
}