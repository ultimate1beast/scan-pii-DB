package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * DTO pour représenter les informations de schéma d'une base de données.
 * Standardisé avec l'architecture BaseResponseDTO.
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SchemaInfoDTO extends BaseResponseDTO {
    
    private String catalogName;
    private String schemaName;
    private List<TableInfoDTO> tables;
    private int totalTableCount;
    private int totalColumnCount;
    private int totalRelationshipCount;
    
    /**
     * DTO pour représenter les informations d'une table.
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfoDTO {
        private String name;
        private String type;
        private String comments;
        private List<ColumnInfoDTO> columns;
        private List<RelationshipDTO> importedRelationships;
        private List<RelationshipDTO> exportedRelationships;
    }
    
    /**
     * DTO pour représenter les informations d'une colonne.
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfoDTO {
        private String name;
        private String type;
        private String comments;
        private int size;
        private int decimalDigits;
        private boolean nullable;
        private boolean primaryKey;
        private boolean foreignKey;
    }
    
    /**
     * DTO pour représenter une relation entre tables.
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipDTO {
        private String pkTable;
        private String fkTable;
        private String pkColumn;
        private String fkColumn;
    }
}