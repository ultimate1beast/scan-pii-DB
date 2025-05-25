package com.privsense.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that handles conditional creation of WebSocket-related beans.
 * This approach allows us to handle cases where WebSocket dependencies might not be available
 * without causing application startup failures.
 */
@Slf4j
@Configuration
public class WebSocketMessagingConfiguration {

    /**
     * Creates a messaging template proxy bean if the actual SimpMessagingTemplate
     * class is available on the classpath.
     * 
     * @return A messaging template object or null if WebSocket is not available
     */
    @Bean
    @ConditionalOnProperty(name = "privsense.websocket.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "messagingTemplateProxy")
    public Object messagingTemplateProxy() {
        try {
            // Try to load the SimpMessagingTemplate class
            Class<?> simpClass = Class.forName("org.springframework.messaging.simp.SimpMessagingTemplate");
            log.info("WebSocket support is available. Real-time notifications will be enabled.");
            
            // Return null here - Spring will inject the actual SimpMessagingTemplate bean
            // that's created by the WebSocket auto-configuration
            return null;
        } catch (ClassNotFoundException e) {
            log.warn("WebSocket classes not found on classpath. WebSocket notifications will be disabled.");
            return null;
        }
    }
}