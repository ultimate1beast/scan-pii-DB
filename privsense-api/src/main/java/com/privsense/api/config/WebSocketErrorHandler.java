package com.privsense.api.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom error handler for WebSocket STOMP messages.
 * Provides better error reporting and management of WebSocket errors.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "privsense.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        log.error("WebSocket client message processing error", ex);
        
        // Extract STOMP headers to provide context about the error
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        StompCommand command = accessor != null ? accessor.getCommand() : null;
        
        // Log detailed error info
        log.error("Error processing message with command {}: {}", command, ex.getMessage());
        
        // Create error message with details
        String errorMessage = "Error processing WebSocket message: " + ex.getMessage();
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }
    
    @Override
    protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload, Throwable cause, StompHeaderAccessor clientHeaderAccessor) {
        // Customize error payload to provide more detailed information
        String detailedError;
        if (cause != null) {
            detailedError = String.format("{\"status\":\"error\",\"message\":\"%s\",\"type\":\"%s\"}", 
                                         cause.getMessage().replace("\"", "\\\""), cause.getClass().getSimpleName());
        } else {
            detailedError = "{\"status\":\"error\",\"message\":\"Unknown WebSocket error\"}";
        }
        
        return super.handleInternal(
            errorHeaderAccessor,
            detailedError.getBytes(StandardCharsets.UTF_8),
            cause, 
            clientHeaderAccessor
        );
    }
}