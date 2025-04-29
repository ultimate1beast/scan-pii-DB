package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import com.privsense.api.dto.result.TableSamplingResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * DTO de réponse pour les opérations d'échantillonnage par lots sur plusieurs tables.
 * Les classes internes ont été extraites dans des fichiers séparés pour améliorer la modularité.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BatchSamplingResponse extends BaseResponseDTO {
    
    private int totalTablesProcessed;
    private int totalColumnsProcessed;
    private long totalExecutionTimeMs;
    private double averageTableTimeMs;
    private List<TableSamplingResult> results;
    
    /**
     * Métriques de performance pour l'opération par lots.
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private int maxConcurrentSamplingTasks;
        private long averageSamplingTimePerColumnMs;
        private long minColumnSamplingTimeMs;
        private long maxColumnSamplingTimeMs;
        private int totalRowsProcessed;
    }
}