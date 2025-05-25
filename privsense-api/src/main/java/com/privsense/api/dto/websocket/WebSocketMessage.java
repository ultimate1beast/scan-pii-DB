package com.privsense.api.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base class for all WebSocket messages.
 * Provides a standardized format for all real-time messages with type and timestamp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class WebSocketMessage {
    private String type;
    private LocalDateTime timestamp = LocalDateTime.now();
}