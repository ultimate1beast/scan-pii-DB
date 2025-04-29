package com.privsense.api.dto.result;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Résultat d'échantillonnage pour une seule table.
 * Cette classe a été extraite de BatchSamplingResponse pour améliorer la modularité.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TableSamplingResult extends BaseResponseDTO {
    private String tableName;
    private int columnCount;
    private long samplingTimeMs;
    private Map<String, ColumnSamplingResult> columnResults;
}