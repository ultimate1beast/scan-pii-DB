package com.privsense.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Data Transfer Object for database connection responses.
 */
@Data
@Builder
public class ConnectionResponse {

    private UUID connectionId;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String databaseProductName;
    private String databaseProductVersion;
    private Boolean sslEnabled;
    private String status;
}