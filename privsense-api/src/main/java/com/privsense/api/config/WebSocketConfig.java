package com.privsense.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket configuration that sets up the message broker and endpoints.
 * This configuration is only enabled if websocket.enabled is set to true in application properties.
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "privsense.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${privsense.websocket.allowed-origins:*}")
    private String[] allowedOrigins;
    
    @Value("${privsense.websocket.heartbeat-in:10000}")
    private long heartbeatIn;
    
    @Value("${privsense.websocket.heartbeat-out:10000}")
    private long heartbeatOut;
    
    // Replace field injection with constructor injection
    private final WebSocketErrorHandler errorHandler;
    
    @Bean
    @Primary  // Mark this as the primary TaskScheduler to address the warning
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("websocket-heartbeat-thread-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker for sending messages to clients
        config.enableSimpleBroker("/topic")
            .setHeartbeatValue(new long[] {heartbeatOut, heartbeatIn})
            .setTaskScheduler(webSocketTaskScheduler());
        
        // Define the prefix for messages that are bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/websocket" endpoint, enabling SockJS fallback options
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .withSockJS()
                .setDisconnectDelay(30L * 1000L) // Cast to long - 30 seconds
                .setHeartbeatTime(25L * 1000L);  // Cast to long - 25 seconds
                
        // Set the custom error handler
        registry.setErrorHandler(errorHandler);
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024)     // 64KB
                    .setSendBufferSizeLimit(512 * 1024) // 512KB
                    .setSendTimeLimit(15 * 1000)        // 15 seconds
                    .addDecoratorFactory(WebSocketConnectionErrorHandler::new); // Use method reference
    }
    
    /**
     * Custom HandshakeInterceptor to handle WebSocket handshake issues
     */
    private static class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // Log the connection attempt which can help troubleshoot issues
            log.debug("WebSocket handshake request from: {}", request.getRemoteAddress());
            
            // HttpHeaders doesn't have setConnectionTimeout, so we'll use session timeout if needed
            // in a future implementation if header manipulation is required
            
            return true; // Allow the handshake to proceed
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                  WebSocketHandler wsHandler, Exception exception) {
            // Log successful handshakes and any exceptions
            if (exception == null) {
                log.debug("WebSocket handshake completed successfully for: {}", request.getRemoteAddress());
            } else {
                log.warn("WebSocket handshake failed for: {}: {}", 
                        request.getRemoteAddress(), exception.getMessage());
            }
        }
    }
}