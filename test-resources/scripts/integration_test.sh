#!/bin/bash
#
# Integration test script for PrivSense application
# This script tests the end-to-end functionality of the PrivSense application
# by simulating a complete workflow: connecting to a test database,
# triggering a scan, and verifying the results.
#

set -e

# Configuration
API_BASE_URL="http://localhost:8080/privsense/api"
TEST_DB_HOST="localhost"
TEST_DB_PORT=3306
TEST_DB_NAME="privsense_test"
TEST_DB_USERNAME="test_user"
TEST_DB_PASSWORD="test_password"

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}PrivSense Integration Test${NC}"
echo -e "${CYAN}-------------------------${NC}"

# Check if API is running
echo "Checking if API is running..."
if ! curl -s "$API_BASE_URL/health" > /dev/null; then
    echo -e "${RED}Error: API is not running. Please start the API service before running this test.${NC}"
    exit 1
else
    echo -e "${GREEN}API is running${NC}"
fi

# Step 1: Create a database connection
echo -e "${BLUE}Step 1: Creating database connection...${NC}"
CONNECTION_RESPONSE=$(curl -s -X POST "$API_BASE_URL/connections" \
  -H "Content-Type: application/json" \
  -d '{
    "host": "'$TEST_DB_HOST'",
    "port": '$TEST_DB_PORT',
    "database": "'$TEST_DB_NAME'",
    "username": "'$TEST_DB_USERNAME'",
    "password": "'$TEST_DB_PASSWORD'",
    "sslEnabled": false
  }')

# Extract connection ID using jq if available, otherwise use grep and sed
if command -v jq > /dev/null; then
    CONNECTION_ID=$(echo $CONNECTION_RESPONSE | jq -r '.connectionId')
else
    CONNECTION_ID=$(echo $CONNECTION_RESPONSE | grep -o '"connectionId":"[^"]*"' | sed 's/"connectionId":"//;s/"//')
fi

if [ -z "$CONNECTION_ID" ]; then
    echo -e "${RED}Error creating connection: $CONNECTION_RESPONSE${NC}"
    exit 1
else
    echo -e "${GREEN}Connection created successfully. Connection ID: $CONNECTION_ID${NC}"
fi

# Step 2: Initiate a scan
echo -e "${BLUE}Step 2: Initiating scan...${NC}"
SCAN_RESPONSE=$(curl -s -X POST "$API_BASE_URL/scan" \
  -H "Content-Type: application/json" \
  -d '{
    "connectionId": "'$CONNECTION_ID'",
    "targetTables": ["users", "orders", "products"],
    "samplingConfig": {
      "sampleSize": 500,
      "samplingMethod": "RANDOM"
    },
    "detectionConfig": {
      "heuristicThreshold": 0.7,
      "regexThreshold": 0.8,
      "nerThreshold": 0.6
    }
  }')

# Extract job ID
if command -v jq > /dev/null; then
    JOB_ID=$(echo $SCAN_RESPONSE | jq -r '.jobId')
else
    JOB_ID=$(echo $SCAN_RESPONSE | grep -o '"jobId":"[^"]*"' | sed 's/"jobId":"//;s/"//')
fi

if [ -z "$JOB_ID" ]; then
    echo -e "${RED}Error initiating scan: $SCAN_RESPONSE${NC}"
    exit 1
else
    echo -e "${GREEN}Scan initiated successfully. Job ID: $JOB_ID${NC}"
fi

# Step 3: Poll for scan completion
echo -e "${BLUE}Step 3: Polling for scan completion...${NC}"
COMPLETED=false
MAX_ATTEMPTS=30
ATTEMPTS=0

while [ "$COMPLETED" = false ] && [ $ATTEMPTS -lt $MAX_ATTEMPTS ]; do
    ATTEMPTS=$((ATTEMPTS+1))
    sleep 5
    STATUS_RESPONSE=$(curl -s -X GET "$API_BASE_URL/scan/$JOB_ID/status")
    
    # Extract status and progress
    if command -v jq > /dev/null; then
        STATUS=$(echo $STATUS_RESPONSE | jq -r '.status')
        PROGRESS=$(echo $STATUS_RESPONSE | jq -r '.progress')
    else
        STATUS=$(echo $STATUS_RESPONSE | grep -o '"status":"[^"]*"' | sed 's/"status":"//;s/"//')
        PROGRESS=$(echo $STATUS_RESPONSE | grep -o '"progress":[0-9.]*' | sed 's/"progress"://')
    fi
    
    echo -e "${YELLOW}Current status: $STATUS - Progress: ${PROGRESS}%${NC}"
    
    if [ "$STATUS" = "COMPLETED" ]; then
        COMPLETED=true
        echo -e "${GREEN}Scan completed successfully!${NC}"
    elif [ "$STATUS" = "FAILED" ]; then
        echo -e "${RED}Scan failed${NC}"
        if command -v jq > /dev/null; then
            ERROR_MSG=$(echo $STATUS_RESPONSE | jq -r '.errorMessage')
            echo -e "${RED}Error message: $ERROR_MSG${NC}"
        fi
        exit 1
    fi
done

if [ "$COMPLETED" = false ]; then
    echo -e "${RED}Scan did not complete within the timeout period.${NC}"
    exit 1
fi

# Step 4: Retrieve the report
echo -e "${BLUE}Step 4: Retrieving compliance report...${NC}"
REPORT_RESPONSE=$(curl -s -X GET "$API_BASE_URL/scan/$JOB_ID/report")

# Extract report summary
if command -v jq > /dev/null; then
    TABLES_SCANNED=$(echo $REPORT_RESPONSE | jq -r '.tablesScanned')
    COLUMNS_SCANNED=$(echo $REPORT_RESPONSE | jq -r '.columnsScanned')
    PII_COLUMNS_FOUND=$(echo $REPORT_RESPONSE | jq -r '.piiColumnsFound')
else
    TABLES_SCANNED=$(echo $REPORT_RESPONSE | grep -o '"tablesScanned":[0-9]*' | sed 's/"tablesScanned"://')
    COLUMNS_SCANNED=$(echo $REPORT_RESPONSE | grep -o '"columnsScanned":[0-9]*' | sed 's/"columnsScanned"://')
    PII_COLUMNS_FOUND=$(echo $REPORT_RESPONSE | grep -o '"piiColumnsFound":[0-9]*' | sed 's/"piiColumnsFound"://')
fi

echo -e "${GREEN}Report retrieved successfully!${NC}"
echo -e "${CYAN}Summary:${NC}"
echo -e "- Tables scanned: ${TABLES_SCANNED}"
echo -e "- Columns scanned: ${COLUMNS_SCANNED}"
echo -e "- PII columns found: ${PII_COLUMNS_FOUND}"

# Save the report to a file
DATE_STAMP=$(date '+%Y%m%d_%H%M%S')
REPORT_FILE="privsense_report_$DATE_STAMP.json"
echo "$REPORT_RESPONSE" > "$REPORT_FILE"
echo -e "${GREEN}Report saved to $REPORT_FILE${NC}"

# Test complete
echo -e "${CYAN}-------------------------${NC}"
echo -e "${GREEN}Integration test completed successfully!${NC}"