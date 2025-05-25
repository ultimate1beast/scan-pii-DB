// Example React component integrating WebSockets and HATEOAS support
import React, { useState, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

/**
 * ScanMonitor component that demonstrates:
 * 1. WebSocket-based real-time status updates
 * 2. HATEOAS link navigation
 * 
 * This component provides a complete example of using both enhancements
 * to create a responsive and discoverable UI.
 */
const ScanMonitor = ({ scanId, authToken }) => {
  // State for scan data and status
  const [scan, setScan] = useState(null);
  const [wsConnected, setWsConnected] = useState(false);
  const [wsClient, setWsClient] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // State for available actions derived from HATEOAS links
  const [availableActions, setAvailableActions] = useState([]);

  // Load initial scan data using HATEOAS links
  useEffect(() => {
    const fetchScanStatus = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await fetch(`/api/v1/scans/${scanId}`, {
          headers: {
            'Accept': 'application/json',
            'Authorization': `Bearer ${authToken}`
          }
        });
        
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        
        const scanData = await response.json();
        setScan(scanData);
        
        // Extract available actions from HATEOAS links
        if (scanData.links && Array.isArray(scanData.links)) {
          const actions = scanData.links.map(link => ({
            name: link.rel,
            href: link.href
          }));
          setAvailableActions(actions);
        }
      } catch (err) {
        setError(`Failed to load scan: ${err.message}`);
        console.error('Error fetching scan:', err);
      } finally {
        setLoading(false);
      }
    };
    
    fetchScanStatus();
  }, [scanId, authToken]);

  // Set up WebSocket connection for real-time updates
  useEffect(() => {
    if (!scanId || !authToken) return;
    
    // Create STOMP client
    const client = new Client({
      brokerURL: 'ws://localhost:8080/privsense/websocket',
      connectHeaders: {
        Authorization: `Bearer ${authToken}`,
      },
      debug: (str) => {
        console.debug(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
    
    // Handle successful connection
    client.onConnect = (frame) => {
      console.log('Connected to WebSocket server');
      setWsConnected(true);
      
      // Subscribe to specific scan updates
      client.subscribe(`/topic/scans/${scanId}`, message => {
        try {
          const messageData = JSON.parse(message.body);
          console.log('Received scan update:', messageData);
          
          // Update scan data with the real-time information
          if (messageData.type === 'SCAN_STATUS_UPDATE' && messageData.data) {
            setScan(prevScan => ({
              ...prevScan,
              ...messageData.data,
              // Preserve links from previous state since WS updates might not include them
              links: prevScan?.links || []
            }));
          }
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e);
        }
      });
      
      // Tell the server we want to subscribe to this scan
      client.publish({
        destination: '/app/subscribe-scan',
        body: JSON.stringify({
          scanId: scanId,
          type: 'SCAN_STATUS'
        })
      });
    };
    
    // Handle errors
    client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers.message);
      setWsConnected(false);
      setError(`WebSocket error: ${frame.headers.message}`);
    };
    
    // Start the connection
    client.activate();
    setWsClient(client);
    
    // Cleanup on unmount
    return () => {
      if (client && client.active) {
        client.deactivate();
      }
    };
  }, [scanId, authToken]);
  
  // Function to navigate using HATEOAS links
  const navigateByRel = useCallback(async (rel) => {
    if (!scan || !scan.links) return;
    
    const link = scan.links.find(l => l.rel === rel);
    if (!link) {
      console.error(`Link with rel "${rel}" not found`);
      return;
    }
    
    try {
      setLoading(true);
      const response = await fetch(link.href, {
        headers: {
          'Accept': 'application/json',
          'Authorization': `Bearer ${authToken}`
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      
      const data = await response.json();
      return data;
    } catch (err) {
      setError(`Failed to navigate to ${rel}: ${err.message}`);
      console.error(`Error navigating to ${rel}:`, err);
    } finally {
      setLoading(false);
    }
  }, [scan, authToken]);
  
  // Function to check if a specific relation link exists
  const hasLink = useCallback((rel) => {
    if (!scan || !scan.links) return false;
    return scan.links.some(link => link.rel === rel);
  }, [scan]);
  
  // Function to cancel scan using DELETE method
  const cancelScan = useCallback(async () => {
    if (!hasLink('cancel')) return;
    
    try {
      setLoading(true);
      const cancelLink = scan.links.find(link => link.rel === 'cancel');
      
      const response = await fetch(cancelLink.href, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      
      // We don't need to update state here as the WebSocket will provide the update
      console.log('Scan cancellation request sent');
    } catch (err) {
      setError(`Failed to cancel scan: ${err.message}`);
      console.error('Error cancelling scan:', err);
    } finally {
      setLoading(false);
    }
  }, [scan, hasLink, authToken]);
  
  // Calculate progress percentage
  const progress = scan && scan.progress ? scan.progress : 
                  scan && scan.completed ? 100 : 0;
  
  // Determine color based on status
  const getStatusColor = () => {
    if (!scan) return 'gray';
    if (scan.completed) return 'green';
    if (scan.failed) return 'red';
    return 'blue';
  };

  return (
    <div className="scan-monitor">
      <h2>Scan Monitor</h2>
      
      {loading && <div className="loading">Loading...</div>}
      {error && <div className="error">{error}</div>}
      
      {/* WebSocket connection status */}
      <div className="connection-status">
        WebSocket: {wsConnected ? 
          <span className="status-connected">Connected</span> : 
          <span className="status-disconnected">Disconnected</span>}
      </div>
      
      {scan && (
        <div className="scan-details">
          <h3>Scan ID: {scan.jobId}</h3>
          
          {/* Status indicator */}
          <div className="status-indicator" style={{ color: getStatusColor() }}>
            Status: {scan.status}
          </div>
          
          {/* Progress bar */}
          {!scan.completed && !scan.failed && (
            <div className="progress-container">
              <div 
                className="progress-bar" 
                style={{ width: `${progress}%`, backgroundColor: getStatusColor() }}
              />
              <span className="progress-text">{progress}%</span>
            </div>
          )}
          
          {/* Scan metadata */}
          <div className="scan-metadata">
            <div>Start Time: {scan.startTime}</div>
            {scan.endTime && <div>End Time: {scan.endTime}</div>}
            {scan.totalColumnsScanned && <div>Columns Scanned: {scan.totalColumnsScanned}</div>}
            {scan.totalPiiColumnsFound && <div>PII Columns Found: {scan.totalPiiColumnsFound}</div>}
          </div>
          
          {/* Dynamically generated actions based on HATEOAS links */}
          <div className="action-buttons">
            {hasLink('results') && (
              <button 
                onClick={() => navigateByRel('results')}
                disabled={loading || !scan.completed}
              >
                View Results
              </button>
            )}
            
            {hasLink('stats') && (
              <button 
                onClick={() => navigateByRel('stats')}
                disabled={loading || !scan.completed}
              >
                View Statistics
              </button>
            )}
            
            {hasLink('tables') && (
              <button 
                onClick={() => navigateByRel('tables')}
                disabled={loading || !scan.completed}
              >
                View Tables
              </button>
            )}
            
            {hasLink('report') && (
              <button 
                onClick={() => navigateByRel('report')}
                disabled={loading || !scan.completed}
              >
                Download Report
              </button>
            )}
            
            {hasLink('cancel') && !scan.completed && !scan.failed && (
              <button 
                onClick={cancelScan}
                disabled={loading}
                className="cancel-button"
              >
                Cancel Scan
              </button>
            )}
          </div>
        </div>
      )}
      
      {/* Available actions from HATEOAS */}
      {availableActions.length > 0 && (
        <div className="available-actions">
          <h4>Available Actions:</h4>
          <ul>
            {availableActions.map(action => (
              <li key={action.name}>{action.name}: {action.href}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default ScanMonitor;