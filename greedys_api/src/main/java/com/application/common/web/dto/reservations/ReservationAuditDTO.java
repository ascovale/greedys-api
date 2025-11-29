package com.application.common.web.dto.reservations;

import java.time.LocalDateTime;
import java.util.List;

import com.application.common.persistence.model.reservation.FieldChange;
import com.application.common.persistence.model.reservation.ReservationAudit.AuditAction;
import com.application.common.persistence.model.reservation.Reservation.UserType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ReservationAudit entity.
 * Used for API responses when returning audit trail information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationAuditDTO {

    /**
     * Unique identifier for the audit record
     */
    private Long id;

    /**
     * ID of the reservation being audited
     */
    private Long reservationId;

    /**
     * Type of action that was performed
     */
    private AuditAction action;

    /**
     * List of field changes
     */
    private List<FieldChange> changedFields;

    /**
     * Reason for the change
     */
    private String changeReason;

    /**
     * Timestamp when the change was made
     */
    private LocalDateTime changedAt;

    /**
     * Username of the user who made the change (null for system)
     */
    private String changedByUsername;

    /**
     * Type of user who made the change
     */
    private UserType changedByUserType;

    /**
     * Display-friendly version of action
     */
    public String getActionDisplay() {
        if (action == null) {
            return "";
        }
        return switch (action) {
            case CREATED -> "Created";
            case UPDATED -> "Updated";
            case STATUS_CHANGED -> "Status Changed";
            case DELETED -> "Deleted";
        };
    }

    /**
     * Get description of changes
     */
    public String getChangesDescription() {
        if (changedFields == null || changedFields.isEmpty()) {
            return "No changes";
        }
        StringBuilder sb = new StringBuilder();
        for (FieldChange change : changedFields) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(change.getField()).append(": ").append(change.getOldValue()).append(" → ").append(change.getNewValue());
        }
        return sb.toString();
    }
}
