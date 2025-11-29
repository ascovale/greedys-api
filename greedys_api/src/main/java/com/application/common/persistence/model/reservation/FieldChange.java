package com.application.common.persistence.model.reservation;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value Object representing a single field change in a reservation audit record.
 * Used for field-level tracking of modifications.
 * 
 * Serializable to/from JSON for storage in ReservationAudit.changedFields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldChange {

    /**
     * Name of the field that changed (e.g., "status", "pax", "reservationDateTime", "tableId").
     */
    @JsonProperty("field")
    private String field;

    /**
     * Previous value before the change (as string representation).
     * Null if there was no previous value (e.g., newly set field).
     */
    @JsonProperty("oldValue")
    private String oldValue;

    /**
     * New value after the change (as string representation).
     */
    @JsonProperty("newValue")
    private String newValue;

    /**
     * Create a FieldChange from old and new values.
     */
    public static FieldChange of(String fieldName, Object oldValue, Object newValue) {
        return FieldChange.builder()
            .field(fieldName)
            .oldValue(oldValue != null ? oldValue.toString() : null)
            .newValue(newValue != null ? newValue.toString() : null)
            .build();
    }
}
