#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Integration test script for PrivSense application
.DESCRIPTION
    This script tests the end-to-end functionality of the PrivSense application
    by simulating a complete workflow: connecting to a test database,
    triggering a scan, and verifying the results.
#>

$ErrorActionPreference = "Stop"
$API_BASE_URL = "http://localhost:8080/privsense/api"
$TEST_DB_HOST = "localhost"
$TEST_DB_PORT = 3306
$TEST_DB_NAME = "privsense_test"
$TEST_DB_USERNAME = "test_user"
$TEST_DB_PASSWORD = "test_password"

Write-Host "PrivSense Integration Test" -ForegroundColor Cyan
Write-Host "-------------------------" -ForegroundColor Cyan

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

# Step 1: Create a database connection
Write-Host "Step 1: Creating database connection..." -ForegroundColor Blue
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

# Step 2: Initiate a scan
Write-Host "Step 2: Initiating scan..." -ForegroundColor Blue
$scanRequest = @{
    connectionId = $connectionId
    targetTables = @("users", "orders", "products") # Example tables, adjust as needed
    samplingConfig = @{
        sampleSize = 500
        samplingMethod = "RANDOM"
    }
    detectionConfig = @{
        heuristicThreshold = 0.7
        regexThreshold = 0.8
        nerThreshold = 0.6
    }
}

try {
    $scanResponse = Invoke-RestMethod -Uri "$API_BASE_URL/scan" -Method Post -ContentType "application/json" -Body ($scanRequest | ConvertTo-Json)
    $jobId = $scanResponse.jobId
    Write-Host "Scan initiated successfully. Job ID: $jobId" -ForegroundColor Green
}
catch {
    Write-Host "Error initiating scan: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Poll for scan completion
Write-Host "Step 3: Polling for scan completion..." -ForegroundColor Blue
$completed = $false
$maxAttempts = 30
$attempts = 0

while (-not $completed -and $attempts -lt $maxAttempts) {
    $attempts++
    try {
        Start-Sleep -Seconds 5
        $statusResponse = Invoke-RestMethod -Uri "$API_BASE_URL/scan/$jobId/status" -Method Get
        Write-Host "Current status: $($statusResponse.status) - Progress: $($statusResponse.progress)%" -ForegroundColor Yellow
        
        if ($statusResponse.status -eq "COMPLETED") {
            $completed = $true
            Write-Host "Scan completed successfully!" -ForegroundColor Green
        }
        elseif ($statusResponse.status -eq "FAILED") {
            Write-Host "Scan failed: $($statusResponse.errorMessage)" -ForegroundColor Red
            exit 1
        }
    }
    catch {
        Write-Host "Error checking scan status: $_" -ForegroundColor Red
        # Continue polling despite error
    }
}

if (-not $completed) {
    Write-Host "Scan did not complete within the timeout period." -ForegroundColor Red
    exit 1
}

# Step 4: Retrieve the report
Write-Host "Step 4: Retrieving compliance report..." -ForegroundColor Blue
try {
    $reportResponse = Invoke-RestMethod -Uri "$API_BASE_URL/scan/$jobId/report" -Method Get
    
    # Display summary information
    Write-Host "Report retrieved successfully!" -ForegroundColor Green
    Write-Host "Summary:" -ForegroundColor Cyan
    Write-Host "- Tables scanned: $($reportResponse.tablesScanned)" -ForegroundColor White
    Write-Host "- Columns scanned: $($reportResponse.columnsScanned)" -ForegroundColor White
    Write-Host "- PII columns found: $($reportResponse.piiColumnsFound)" -ForegroundColor White
    
    # Save the report to a file
    $dateStamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $reportFile = "privsense_report_$dateStamp.json"
    $reportResponse | ConvertTo-Json -Depth 10 | Out-File -FilePath $reportFile
    Write-Host "Report saved to $reportFile" -ForegroundColor Green
}
catch {
    Write-Host "Error retrieving report: $_" -ForegroundColor Red
    exit 1
}

# Test complete
Write-Host "-------------------------" -ForegroundColor Cyan
Write-Host "Integration test completed successfully!" -ForegroundColor Green