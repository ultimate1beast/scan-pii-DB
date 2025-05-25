package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.SchemaInfoDTO;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;

import org.mapstruct.Mapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper spécialisé pour les métadonnées de schéma de base de données.
 * Utilise la configuration centralisée MapStructConfig.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class,
    builder = @org.mapstruct.Builder(disableBuilder = false)
)
public interface SchemaMapper {

    /**
     * Map SchemaInfo vers SchemaInfoDTO.
     */
    @Mapping(source = "catalogName", target = "catalogName")
    @Mapping(source = "schemaName", target = "schemaName")
    @Mapping(source = "tables", target = "tables")
    SchemaInfoDTO toSchemaDto(SchemaInfo schemaInfo);
    
    /**
     * Add metadata after mapping is complete
     */
    @AfterMapping
    default void addMetadata(@MappingTarget SchemaInfoDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
    /**
     * Map TableInfo vers TableInfoDTO.
     */
    @Mapping(source = "tableName", target = "name")
    @Mapping(source = "tableType", target = "type")
    @Mapping(source = "remarks", target = "comments")
    @Mapping(source = "columns", target = "columns")
    @Mapping(source = "importedRelationships", target = "importedRelationships")
    @Mapping(source = "exportedRelationships", target = "exportedRelationships")
    SchemaInfoDTO.TableInfoDTO toTableDto(TableInfo tableInfo);
    
    /**
     * Map ColumnInfo vers ColumnInfoDTO.
     */
    @Mapping(source = "columnName", target = "name")
    @Mapping(source = "databaseTypeName", target = "type")
    @Mapping(source = "comments", target = "comments")
    @Mapping(source = "size", target = "size")
    @Mapping(source = "scale", target = "decimalDigits")
    @Mapping(source = "nullable", target = "nullable")
    @Mapping(source = "primaryKey", target = "primaryKey")
    @Mapping(target = "foreignKey", expression = "java(columnInfo.getTable() != null && !columnInfo.getTable().getImportedRelationships().isEmpty())")
    SchemaInfoDTO.ColumnInfoDTO toColumnDto(ColumnInfo columnInfo);
    
    /**
     * Map RelationshipInfo vers RelationshipDTO.
     */
    @Mapping(source = "sourceTable.tableName", target = "pkTable")
    @Mapping(source = "targetTable.tableName", target = "fkTable")
    @Mapping(source = "sourceColumn.columnName", target = "pkColumn")
    @Mapping(source = "targetColumn.columnName", target = "fkColumn")
    SchemaInfoDTO.RelationshipDTO toRelationshipDto(RelationshipInfo relationshipInfo);
}