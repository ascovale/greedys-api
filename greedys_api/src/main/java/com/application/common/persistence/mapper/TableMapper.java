package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.restaurant.TableDTO;
import com.application.restaurant.persistence.model.Table;
import com.application.restaurant.web.dto.restaurant.NewTableDTO;

/**
 * MapStruct mapper per la conversione tra Table e TableDTO/NewTableDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface TableMapper {

    /**
     * Converte un'entità Table in TableDTO
     */
    @Mapping(target = "room", ignore = true)
    TableDTO toDTO(Table table);

    /**
     * Converte un NewTableDTO in entità Table
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true) // Sarà impostato dal service utilizzando roomId
    Table fromNewTableDTO(NewTableDTO newTableDTO);

    /**
     * Aggiorna un'entità Table esistente con i dati dal NewTableDTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    void updateEntityFromNewDTO(NewTableDTO dto, @MappingTarget Table entity);
}
