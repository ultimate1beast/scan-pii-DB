package com.privsense.api.config;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import lombok.extern.slf4j.Slf4j;

/**
 * Decorator for WebSocketHandler that adds robust error handling for transport-level
 * WebSocket issues, including connection errors and abrupt disconnections.
 */
@Slf4j
public class WebSocketConnectionErrorHandler extends WebSocketHandlerDecorator {
    
    public WebSocketConnectionErrorHandler(WebSocketHandler delegate) {
        super(delegate);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            log.debug("WebSocket connection established: {}", session.getId());
            super.afterConnectionEstablished(session);
        } catch (Exception e) {
            log.error("Error after WebSocket connection established: {}", e.getMessage(), e);
            closeSessionSafely(session, new CloseStatus(1011, "Server error during connection setup"));
            throw e;
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            super.handleMessage(session, message);
        } catch (Exception e) {
            log.warn("Error handling WebSocket message from session {}: {}", 
                    session.getId(), e.getMessage());
            
            // For payload format errors, close with appropriate status
            if (e.getMessage() != null && e.getMessage().contains("Invalid")) {
                closeSessionSafely(session, new CloseStatus(1007, "Invalid data format"));
            } else {
                closeSessionSafely(session, new CloseStatus(1011, "Server error processing message"));
            }
            
            // Don't propagate exception for binary message errors
            if (message != null && !message.isLast() && e.getMessage() != null && 
                e.getMessage().contains("Invalid character")) {
                log.debug("Suppressing binary message exception: {}", e.getMessage());
            } else {
                throw e;
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error on session {}: {}", 
                session.getId(), exception.getMessage());
        
        try {
            super.handleTransportError(session, exception);
        } catch (Exception e) {
            log.error("Error handling WebSocket transport error: {}", e.getMessage(), e);
        } finally {
            closeSessionSafely(session, CloseStatus.SERVER_ERROR);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        try {
            log.debug("WebSocket connection closed: {} with status: {}", 
                    session.getId(), closeStatus);
            super.afterConnectionClosed(session, closeStatus);
        } catch (Exception e) {
            log.error("Error after WebSocket connection closed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Safely closes a WebSocket session, handling any exceptions that might occur
     */
    private void closeSessionSafely(WebSocketSession session, CloseStatus status) {
        if (session != null && session.isOpen()) {
            try {
                session.close(status);
            } catch (Exception e) {
                log.warn("Error closing WebSocket session: {}", e.getMessage());
            }
        }
    }
}