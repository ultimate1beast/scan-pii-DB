package com.privsense.api.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to subscribe to specific resource updates.
 * Used when clients want to receive real-time updates for a specific scan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    private String scanId;
    private String type = "SCAN_STATUS";
}