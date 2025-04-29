package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.SchemaInfoDTO;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper spécialisé pour les métadonnées de schéma de base de données.
 * Utilise la configuration centralisée MapStructConfig.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class
)
public interface SchemaMapper {

    /**
     * Map SchemaInfo vers SchemaInfoDTO.
     */
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    SchemaInfoDTO toSchemaDto(SchemaInfo schemaInfo);
    
    /**
     * Map TableInfo vers TableInfoDTO.
     */
    SchemaInfoDTO.TableInfoDTO toTableDto(TableInfo tableInfo);
    
    /**
     * Map ColumnInfo vers ColumnInfoDTO.
     */
    SchemaInfoDTO.ColumnInfoDTO toColumnDto(ColumnInfo columnInfo);
    
    /**
     * Map RelationshipInfo vers RelationshipDTO.
     */
    SchemaInfoDTO.RelationshipDTO toRelationshipDto(RelationshipInfo relationshipInfo);
}