package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.application.common.web.dto.get.TableDTO;
import com.application.restaurant.persistence.model.Table;
import com.application.restaurant.web.dto.post.NewTableDTO;

/**
 * MapStruct mapper per la conversione tra Table e TableDTO/NewTableDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface TableMapper {

    TableMapper INSTANCE = Mappers.getMapper(TableMapper.class);

    /**
     * Converte un'entità Table in TableDTO
     */
    TableDTO toDTO(Table table);

    /**
     * Converte un NewTableDTO in entità Table
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true) // Sarà impostato dal service
    Table fromNewTableDTO(NewTableDTO newTableDTO);

    /**
     * Converte un TableDTO in entità Table
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true) // Sarà impostato dal service
    Table toEntity(TableDTO tableDTO);

    /**
     * Aggiorna un'entità Table esistente con i dati dal TableDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    void updateEntityFromDTO(TableDTO dto, @MappingTarget Table entity);

    /**
     * Aggiorna un'entità Table esistente con i dati dal NewTableDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    void updateEntityFromNewDTO(NewTableDTO dto, @MappingTarget Table entity);
}
