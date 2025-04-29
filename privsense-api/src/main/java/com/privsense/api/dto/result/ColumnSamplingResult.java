package com.privsense.api.dto.result;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Résultat d'échantillonnage pour une seule colonne.
 * Cette classe a été extraite de BatchSamplingResponse pour améliorer la modularité.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ColumnSamplingResult extends BaseResponseDTO {
    private String columnName;
    private int sampleSize;
    private String samplingMethod;
    private int actualRowCount;
    private int nullCount;
    private double nullPercentage;
    private double nonNullPercentage;
    private Double entropy;
    private boolean entropyCalculated;
    private Map<String, Long> topValues; // Top N valeurs les plus communes et leur nombre
}