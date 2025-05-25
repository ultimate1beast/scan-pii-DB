package com.privsense.api.dto.websocket;

import com.privsense.api.dto.ScanJobResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * WebSocket message for scan status updates.
 * Contains complete information about a scan's current status.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ScanStatusMessage extends WebSocketMessage {
    private ScanJobResponse data;
    
    public ScanStatusMessage(ScanJobResponse data) {
        super("SCAN_STATUS_UPDATE", null);
        this.data = data;
    }
}