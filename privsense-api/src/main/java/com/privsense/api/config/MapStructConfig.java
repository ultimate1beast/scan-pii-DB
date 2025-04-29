package com.privsense.api.config;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Configuration centralisée pour MapStruct qui remplace l'ancienne approche ModelMapper.
 * Cette classe définit des options communes pour tous les mappeurs MapStruct.
 */
@MapperConfig(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MapStructConfig {
    // Cette interface ne contient aucune méthode, elle sert uniquement à porter les annotations de configuration
}