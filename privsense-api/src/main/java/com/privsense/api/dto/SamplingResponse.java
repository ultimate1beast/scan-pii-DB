package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * DTO pour les réponses d'échantillonnage.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SamplingResponse extends BaseResponseDTO {
    
    private String tableName;
    private String columnName;
    private int sampleSize;
    private String samplingMethod;
    private int actualRowCount;
    private int nullCount;
    private double nullPercentage;
    private double nonNullPercentage;
    private Double entropy;
    private boolean entropyCalculated;
    private List<String> sampleValues;
    private long samplingTimeMs;
    private Map<String, Long> valueDistribution;
}