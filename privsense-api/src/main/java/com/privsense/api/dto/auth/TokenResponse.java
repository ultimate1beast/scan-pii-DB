package com.privsense.api.dto.auth;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Response DTO containing JWT tokens after successful authentication.
 * Extends BaseResponseDTO to include standard metadata in the response.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TokenResponse extends BaseResponseDTO {

    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String tokenType;
    private String username;
    private String[] roles;
    private Map<String, String> securityHeaders;

    public TokenResponse() {
        this.tokenType = "Bearer";
        this.securityHeaders = new HashMap<>();
        addMeta("status", "SUCCESS");
    }
}