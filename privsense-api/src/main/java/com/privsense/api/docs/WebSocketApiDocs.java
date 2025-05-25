/**
 * WebSocket API Documentation.
 * This contains detailed information about real-time communication options.
 */
package com.privsense.api.docs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket and HATEOAS documentation.
 * This class exists purely to provide documentation for the WebSocket API
 * and HATEOAS capabilities through OpenAPI.
 */
@OpenAPIDefinition(
    tags = {
        @Tag(name = "WebSockets", description = "Real-time API using WebSockets")
    }
)
@Configuration
public class WebSocketApiDocs {

    /**
     * Creates documentation group for WebSocket API.
     * This allows the WebSocket API docs to be displayed separately in the Swagger UI.
     */
    @Bean
    public GroupedOpenApi webSocketApiDoc() {
        return GroupedOpenApi.builder()
                .group("websocket-api")
                .pathsToMatch("/websocket/**")
                .displayName("WebSocket API")
                .build();
    }
    
    /**
     * Example WebSocket client connection:
     * 
     * <pre>
     * // Create STOMP client
     * const client = new Client({
     *   brokerURL: 'ws://localhost:8080/privsense/websocket',
     *   connectHeaders: {
     *     Authorization: `Bearer ${authToken}`,
     *   },
     *   debug: function (str) {
     *     console.debug(str);
     *   },
     *   reconnectDelay: 5000,
     *   heartbeatIncoming: 10000,
     *   heartbeatOutgoing: 10000,
     * });
     * 
     * // Connection established handler
     * client.onConnect = function(frame) {
     *   console.log('Connected to WebSocket server');
     *   
     *   // Subscribe to specific scan updates
     *   client.subscribe(`/topic/scans/${scanId}`, message => {
     *     const messageData = JSON.parse(message.body);
     *     console.log('Received scan update:', messageData);
     *   });
     *   
     *   // Tell the server we want to subscribe to this scan
     *   client.publish({
     *     destination: '/app/subscribe-scan',
     *     body: JSON.stringify({
     *       scanId: scanId,
     *       type: 'SCAN_STATUS'
     *     })
     *   });
     * };
     * 
     * // Activate the connection
     * client.activate();
     * </pre>
     */
    public void webSocketClientExample() {
        // This method exists only for documentation purposes
    }
    
    /**
     * WebSocket Endpoints:
     * 
     * <pre>
     * WebSocket Connection Endpoint: 
     * /websocket
     * 
     * Available Subscription Topics:
     * - /topic/scans - All scan updates across the system
     * - /topic/scans/{scanId} - Updates for a specific scan
     * - /user/queue/messages - User-specific direct messages
     * 
     * Message Destinations:
     * - /app/subscribe-scan - Subscribe to updates for a specific scan
     * - /app/subscribe/{jobId} - Legacy subscription endpoint
     * - /app/notify - Send a notification to the server
     * </pre>
     */
    public void webSocketEndpoints() {
        // This method exists only for documentation purposes
    }
    
    /**
     * Message Formats:
     * 
     * <pre>
     * Scan Status Message:
     * {
     *   "type": "SCAN_STATUS_UPDATE",
     *   "timestamp": "2025-05-18T14:35:23.455",
     *   "data": {
     *     "jobId": "123e4567-e89b-12d3-a456-426614174000",
     *     "status": "RUNNING",
     *     "progress": 45,
     *     "startTime": "2025-05-18T14:30:00",
     *     "estimatedCompletionTime": "2025-05-18T14:40:00",
     *     "detectionCount": 127
     *   }
     * }
     * 
     * Subscription Request:
     * {
     *   "scanId": "123e4567-e89b-12d3-a456-426614174000",
     *   "type": "SCAN_STATUS"
     * }
     * </pre>
     */
    public void webSocketMessageFormats() {
        // This method exists only for documentation purposes
    }
    
    /**
     * HATEOAS Support Documentation
     * 
     * <pre>
     * PrivSense API follows HATEOAS principles to allow clients to navigate 
     * the API dynamically. Each resource includes a 'links' array containing
     * related resources.
     * 
     * Example Response with HATEOAS links:
     * {
     *   "jobId": "123e4567-e89b-12d3-a456-426614174000",
     *   "status": "COMPLETED",
     *   "meta": {
     *     "timestamp": "2025-05-18T15:30:00",
     *     "status": "SUCCESS"
     *   },
     *   "links": [
     *     {
     *       "rel": "self",
     *       "href": "/api/v1/scans/123e4567-e89b-12d3-a456-426614174000"
     *     },
     *     {
     *       "rel": "results",
     *       "href": "/api/v1/scans/123e4567-e89b-12d3-a456-426614174000/results"
     *     }
     *   ]
     * }
     * 
     * Standard Link Relations:
     * - self: The current resource
     * - collection: The collection containing this resource
     * - next/prev/first/last: For paginated resources
     * - results: For scan results
     * - report: For scan reports
     * - stats: For scan statistics
     * - tables: For scanned tables
     * - columns: For table columns
     * </pre>
     */
    public void hateoasSupport() {
        // This method exists only for documentation purposes
    }
}