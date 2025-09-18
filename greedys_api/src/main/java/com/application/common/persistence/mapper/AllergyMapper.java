package com.application.common.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.application.common.web.dto.customer.AllergyDTO;
import com.application.common.web.dto.customer.NewAllergyDTO;
import com.application.customer.persistence.model.Allergy;

/**
 * MapStruct mapper per la conversione tra Allergy e AllergyDTO/NewAllergyDTO
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface AllergyMapper {

    /**
     * Converte un'entità Allergy in AllergyDTO
     */
    AllergyDTO toDTO(Allergy allergy);

    /**
     * Converte un NewAllergyDTO in entità Allergy
     */
    @Mapping(target = "id", ignore = true)
    Allergy fromNewAllergyDTO(NewAllergyDTO newAllergyDTO);


    /**
     * Aggiorna un'entità Allergy esistente con i dati dal NewAllergyDTO
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromNewDTO(NewAllergyDTO dto, @MappingTarget Allergy entity);
}
