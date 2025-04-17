#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Performance test script for PrivSense application
.DESCRIPTION
    This script tests the performance of the PrivSense application by running
    multiple scans with different configuration parameters and measuring execution times.
    It helps identify bottlenecks in the application's processing pipeline.
#>

$ErrorActionPreference = "Stop"
$API_BASE_URL = "http://localhost:8080/privsense/api"
$TEST_DB_HOST = "localhost"
$TEST_DB_PORT = 3306
$TEST_DB_NAME = "privsense_test"
$TEST_DB_USERNAME = "test_user"
$TEST_DB_PASSWORD = "test_password"
$RESULTS_FILE = "performance_results_$(Get-Date -Format 'yyyyMMdd_HHmmss').csv"

Write-Host "PrivSense Performance Test" -ForegroundColor Cyan
Write-Host "---------------------------" -ForegroundColor Cyan

# Check if API is running
try {
    Write-Host "Checking if API is running..."
    $response = Invoke-RestMethod -Uri "$API_BASE_URL/health" -Method Get
    Write-Host "API is running: $($response.status)" -ForegroundColor Green
}
catch {
    Write-Host "Error: API is not running. Please start the API service before running this test." -ForegroundColor Red
    exit 1
}

# Create a database connection
Write-Host "Creating database connection..." -ForegroundColor Blue
$connectionRequest = @{
    host = $TEST_DB_HOST
    port = $TEST_DB_PORT
    database = $TEST_DB_NAME
    username = $TEST_DB_USERNAME
    password = $TEST_DB_PASSWORD
    sslEnabled = $false
}

try {
    $connectionResponse = Invoke-RestMethod -Uri "$API_BASE_URL/connections" -Method Post -ContentType "application/json" -Body ($connectionRequest | ConvertTo-Json)
    $connectionId = $connectionResponse.connectionId
    Write-Host "Connection created successfully. Connection ID: $connectionId" -ForegroundColor Green
}
catch {
    Write-Host "Error creating connection: $_" -ForegroundColor Red
    exit 1
}

# Write CSV header
"TestRun,SampleSize,MaxConcurrency,SamplingMethod,TablesScanned,ColumnsScanned,TotalTimeMs,MetadataTimeMs,SamplingTimeMs,DetectionTimeMs,ReportingTimeMs" | Out-File -FilePath $RESULTS_FILE

# Test configurations
$testConfigurations = @(
    @{
        name = "Baseline (Default Settings)"
        sampleSize = 1000
        maxConcurrentQueries = 5
        samplingMethod = "RANDOM"
    },
    @{
        name = "Small Sample Size"
        sampleSize = 100
        maxConcurrentQueries = 5
        samplingMethod = "RANDOM"
    },
    @{
        name = "Large Sample Size"
        sampleSize = 5000
        maxConcurrentQueries = 5
        samplingMethod = "RANDOM"
    },
    @{
        name = "High Concurrency"
        sampleSize = 1000
        maxConcurrentQueries = 10
        samplingMethod = "RANDOM"
    },
    @{
        name = "Low Concurrency"
        sampleSize = 1000
        maxConcurrentQueries = 2
        samplingMethod = "RANDOM"
    }
)

$testRun = 1

foreach ($config in $testConfigurations) {
    Write-Host "`nTest Configuration: $($config.name)" -ForegroundColor Cyan
    Write-Host "- Sample Size: $($config.sampleSize)" -ForegroundColor White
    Write-Host "- Max Concurrent Queries: $($config.maxConcurrentQueries)" -ForegroundColor White
    Write-Host "- Sampling Method: $($config.samplingMethod)" -ForegroundColor White
    
    # Create scan request
    $scanRequest = @{
        connectionId = $connectionId
        targetTables = @("users", "orders", "products") # Example tables, adjust as needed
        samplingConfig = @{
            sampleSize = $config.sampleSize
            samplingMethod = $config.samplingMethod
            maxConcurrentDbQueries = $config.maxConcurrentQueries
        }
        detectionConfig = @{
            heuristicThreshold = 0.7
            regexThreshold = 0.8
            nerThreshold = 0.6
        }
    }

    Write-Host "Starting scan..." -ForegroundColor Blue
    $startTime = Get-Date
    
    try {
        $scanResponse = Invoke-RestMethod -Uri "$API_BASE_URL/scan" -Method Post -ContentType "application/json" -Body ($scanRequest | ConvertTo-Json)
        $jobId = $scanResponse.jobId
        Write-Host "Scan initiated. Job ID: $jobId" -ForegroundColor Green
    }
    catch {
        Write-Host "Error initiating scan: $_" -ForegroundColor Red
        continue
    }
    
    # Poll for scan completion
    Write-Host "Polling for scan completion..." -ForegroundColor Blue
    $completed = $false
    $maxAttempts = 60
    $attempts = 0
    $lastStatus = ""
    $lastProgress = 0
    $phaseStartTimes = @{}
    $phaseDurations = @{
        "EXTRACTING_METADATA" = 0
        "SAMPLING_DATA" = 0
        "DETECTING_PII" = 0
        "GENERATING_REPORT" = 0
    }

    while (-not $completed -and $attempts -lt $maxAttempts) {
        $attempts++
        try {
            Start-Sleep -Seconds 2
            $statusResponse = Invoke-RestMethod -Uri "$API_BASE_URL/scan/$jobId/status" -Method Get
            $currentStatus = $statusResponse.status
            $currentOperation = $statusResponse.currentOperation
            
            # Record phase start times for performance measurement
            if ($currentStatus -ne $lastStatus -and $phaseDurations.ContainsKey($currentStatus)) {
                $phaseStartTimes[$currentStatus] = Get-Date
                if ($lastStatus -ne "") {
                    if ($phaseStartTimes.ContainsKey($lastStatus)) {
                        $phaseDuration = (Get-Date) - $phaseStartTimes[$lastStatus]
                        $phaseDurations[$lastStatus] = $phaseDuration.TotalMilliseconds
                        Write-Host "Phase $lastStatus completed in $($phaseDuration.TotalSeconds.ToString("F2")) seconds" -ForegroundColor Yellow
                    }
                }
            }
            
            if ($statusResponse.progress -ne $lastProgress -or $currentStatus -ne $lastStatus) {
                Write-Host "Status: $currentStatus - Progress: $($statusResponse.progress)%" -ForegroundColor Yellow
                $lastProgress = $statusResponse.progress
                $lastStatus = $currentStatus
            }
            
            if ($currentStatus -eq "COMPLETED") {
                $completed = $true
                $endTime = Get-Date
                $duration = $endTime - $startTime
                
                Write-Host "Scan completed successfully in $($duration.TotalSeconds.ToString("F2")) seconds!" -ForegroundColor Green
                
                # Get the report
                $reportResponse = Invoke-RestMethod -Uri "$API_BASE_URL/scan/$jobId/report" -Method Get
                
                # Calculate the last phase duration
                if ($phaseStartTimes.ContainsKey($lastStatus)) {
                    $phaseDuration = $endTime - $phaseStartTimes[$lastStatus]
                    $phaseDurations[$lastStatus] = $phaseDuration.TotalMilliseconds
                }
                
                # Write results to CSV
                $resultLine = "$testRun,$($config.sampleSize),$($config.maxConcurrentQueries),$($config.samplingMethod),$($reportResponse.tablesScanned),$($reportResponse.columnsScanned),$($duration.TotalMilliseconds),$($phaseDurations['EXTRACTING_METADATA']),$($phaseDurations['SAMPLING_DATA']),$($phaseDurations['DETECTING_PII']),$($phaseDurations['GENERATING_REPORT'])"
                $resultLine | Out-File -FilePath $RESULTS_FILE -Append
                
                # Display detailed performance information
                Write-Host "Performance Details:" -ForegroundColor Cyan
                Write-Host "- Total Time: $($duration.TotalSeconds.ToString("F2")) seconds" -ForegroundColor White
                Write-Host "- Metadata Extraction: $($phaseDurations['EXTRACTING_METADATA'] / 1000) seconds" -ForegroundColor White
                Write-Host "- Data Sampling: $($phaseDurations['SAMPLING_DATA'] / 1000) seconds" -ForegroundColor White
                Write-Host "- PII Detection: $($phaseDurations['DETECTING_PII'] / 1000) seconds" -ForegroundColor White
                Write-Host "- Report Generation: $($phaseDurations['GENERATING_REPORT'] / 1000) seconds" -ForegroundColor White
                Write-Host "- Tables Scanned: $($reportResponse.tablesScanned)" -ForegroundColor White
                Write-Host "- Columns Scanned: $($reportResponse.columnsScanned)" -ForegroundColor White
                Write-Host "- PII Columns Found: $($reportResponse.piiColumnsFound)" -ForegroundColor White
            }
            elseif ($currentStatus -eq "FAILED") {
                Write-Host "Scan failed: $($statusResponse.errorMessage)" -ForegroundColor Red
                break
            }
        }
        catch {
            Write-Host "Error checking scan status: $_" -ForegroundColor Red
        }
    }
    
    if (-not $completed) {
        Write-Host "Scan did not complete within the timeout period." -ForegroundColor Red
    }
    
    $testRun++
}

Write-Host "`nPerformance testing completed!" -ForegroundColor Green
Write-Host "Results saved to $RESULTS_FILE" -ForegroundColor Green
Write-Host "Use these results to identify bottlenecks and optimize the application." -ForegroundColor Cyan