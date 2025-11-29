package com.application.common.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.application.common.persistence.model.reservation.FieldChange;
import com.application.common.persistence.model.reservation.ReservationAudit;
import com.application.common.web.dto.reservations.ReservationAuditDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MapStruct mapper for ReservationAudit entity to DTO conversion.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReservationAuditMapper {

    /**
     * Convert ReservationAudit entity to ReservationAuditDTO
     */
    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "changedByUsername", source = "changedBy.username")
    @Mapping(target = "changedFields", expression = "java(parseChangedFields(audit.getChangedFields()))")
    ReservationAuditDTO toDTO(ReservationAudit audit);

    /**
     * Helper method to parse JSON string to List<FieldChange>
     */
    default List<FieldChange> parseChangedFields(String changedFieldsJson) {
        if (changedFieldsJson == null || changedFieldsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(changedFieldsJson, new TypeReference<List<FieldChange>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
