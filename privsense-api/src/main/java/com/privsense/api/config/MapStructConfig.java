package com.privsense.api.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.Builder;

/**
 * Centralized MapStruct configuration.
 * This configuration is used by all mapper interfaces to ensure consistent behavior.
 */
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    builder = @Builder(disableBuilder = false)
)
public interface MapStructConfig {
    // Configuration interface, no methods required
}