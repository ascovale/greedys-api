package com.application.common.service.reservation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.ReservationAuditDAO;
import com.application.common.persistence.mapper.ReservationAuditMapper;
import com.application.common.persistence.model.reservation.FieldChange;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.ReservationAudit;
import com.application.common.persistence.model.reservation.ReservationAudit.AuditAction;
import com.application.common.web.dto.reservations.ReservationAuditDTO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.restaurant.persistence.model.user.RUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing reservation audit trail.
 * Tracks all changes to reservations for compliance and history purposes.
 * 
 * ⭐ Design Pattern: Audit-per-change (one row per modification)
 * - Each change creates a single ReservationAudit row
 * - Field changes stored as JSON array in changed_fields column
 * - Audit triggers: datetime, pax, status, notes, tableId, serviceId
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationAuditService {

    private final ReservationAuditDAO auditDAO;
    private final ReservationDAO reservationDAO;
    private final ReservationAuditMapper auditMapper;

    /**
     * Convert List<FieldChange> to JSON string for database storage
     */
    private String changesListToJson(List<FieldChange> changes) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(changes);
        } catch (Exception e) {
            log.warn("Failed to serialize changes to JSON", e);
            return "[]";
        }
    }

    /**
     * Record reservation creation in audit trail
     * 
     * @param reservation The newly created reservation
     * @param changedByUser User who created the reservation (nullable for system-created)
     */
    @Transactional
    public void recordCreated(Reservation reservation, RUser changedByUser) {
        log.debug("Recording reservation creation: reservation_id={}", reservation.getId());
        
        List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange("pax", null, reservation.getPax().toString()));
        changes.add(new FieldChange("kids", null, reservation.getKids().toString()));
        changes.add(new FieldChange("reservationDateTime", null, reservation.getReservationDateTime().toString()));
        changes.add(new FieldChange("status", null, reservation.getStatus().toString()));
        
        if (reservation.getNotes() != null) {
            changes.add(new FieldChange("notes", null, reservation.getNotes()));
        }
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.CREATED)
                .changedFields(changesListToJson(changes))
                .changeReason("Reservation created")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Created audit record for reservation {}", reservation.getId());
    }

    /**
     * Record reservation status change
     * 
     * @param reservation The reservation
     * @param oldStatus Previous status
     * @param newStatus New status
     * @param reason Reason for change
     * @param changedByUser User making the change
     */
    @Transactional
    public void recordStatusChanged(Reservation reservation, Reservation.Status oldStatus, 
                                    Reservation.Status newStatus, String reason, 
                                    RUser changedByUser) {
        log.debug("Recording status change for reservation {}: {} -> {}", 
                  reservation.getId(), oldStatus, newStatus);
        
        List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange("status", oldStatus.toString(), newStatus.toString()));
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.STATUS_CHANGED)
                .changedFields(changesListToJson(changes))
                .changeReason(reason != null ? reason : "Status changed to " + newStatus)
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded status change for reservation {}: {} -> {}", 
                 reservation.getId(), oldStatus, newStatus);
    }

    /**
     * Record reservation update (pax, time, notes, etc.)
     * 
     * @param reservation The reservation
     * @param changes Map of field changes (fieldName -> [oldValue, newValue])
     * @param reason Reason for update
     * @param changedByUser User making the change
     */
    @Transactional
    public void recordUpdated(Reservation reservation, List<FieldChange> changes, String reason,
                              RUser changedByUser) {
        log.debug("Recording update for reservation {} with {} field changes", 
                  reservation.getId(), changes.size());
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.UPDATED)
                .changedFields(changesListToJson(changes))
                .changeReason(reason != null ? reason : "Reservation updated")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded update for reservation {}", reservation.getId());
    }

    /**
     * Record reservation deletion
     * 
     * @param reservation The reservation
     * @param reason Reason for deletion
     * @param changedByUser User making the deletion
     */
    @Transactional
    public void recordDeleted(Reservation reservation, String reason,
                              RUser changedByUser) {
        log.debug("Recording deletion for reservation {}", reservation.getId());
        
        List<FieldChange> changes = new ArrayList<>();
        changes.add(new FieldChange("status", reservation.getStatus().toString(), "DELETED"));
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.DELETED)
                .changedFields(changesListToJson(changes))
                .changeReason(reason != null ? reason : "Reservation deleted")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded deletion for reservation {}", reservation.getId());
    }

    /**
     * Record reservation modification requested by customer
     * 
     * @param reservation The reservation being modified
     * @param originalValues The original values before modification request
     * @param requestedValues The requested new values
     * @param reason Customer's reason for requesting modification
     * @param changedByUser User (customer) making the request
     */
    @Transactional
    public void recordModificationRequested(Reservation reservation, 
                                           List<FieldChange> changes,
                                           String reason,
                                           RUser changedByUser) {
        log.debug("Recording modification request for reservation {}", reservation.getId());
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.MODIFICATION_REQUESTED)
                .changedFields(changesListToJson(changes))
                .changeReason(reason != null ? reason : "Modification requested by customer")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded modification request for reservation {}", reservation.getId());
    }

    /**
     * Record reservation modification approved by restaurant
     * 
     * @param reservation The reservation
     * @param changes The field changes that were approved
     * @param reason Reason for approval
     * @param changedByUser User (restaurant staff) approving the modification
     */
    @Transactional
    public void recordModificationApproved(Reservation reservation,
                                          List<FieldChange> changes,
                                          String reason,
                                          RUser changedByUser) {
        log.debug("Recording modification approval for reservation {}", reservation.getId());
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.MODIFICATION_APPROVED)
                .changedFields(changesListToJson(changes))
                .changeReason(reason != null ? reason : "Modification approved by restaurant staff")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded modification approval for reservation {}", reservation.getId());
    }

    /**
     * Record reservation modification rejected by restaurant
     * 
     * @param reservation The reservation
     * @param requestedChanges The field changes that were rejected
     * @param reason Reason for rejection
     * @param changedByUser User (restaurant staff) rejecting the modification
     */
    @Transactional
    public void recordModificationRejected(Reservation reservation,
                                          List<FieldChange> requestedChanges,
                                          String reason,
                                          RUser changedByUser) {
        log.debug("Recording modification rejection for reservation {}", reservation.getId());
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.MODIFICATION_REJECTED)
                .changedFields(changesListToJson(requestedChanges))
                .changeReason(reason != null ? reason : "Modification rejected by restaurant staff")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded modification rejection for reservation {}", reservation.getId());
    }

    /**
     * Record reservation modified directly by restaurant staff
     * (Not through customer request, direct edit by staff)
     * 
     * @param reservation The reservation
     * @param changes The field changes applied
     * @param reason Reason for modification
     * @param changedByUser User (restaurant staff) making the direct modification
     */
    @Transactional
    public void recordModifiedByRestaurant(Reservation reservation,
                                          List<FieldChange> changes,
                                          String reason,
                                          RUser changedByUser) {
        log.debug("Recording direct modification for reservation {}", reservation.getId());
        
        ReservationAudit audit = ReservationAudit.builder()
                .reservation(reservation)
                .action(AuditAction.MODIFIED_BY_RESTAURANT)
                .changedFields(changesListToJson(changes))
                .changeReason(reason != null ? reason : "Modified directly by restaurant staff")
                .changedAt(LocalDateTime.now())
                .changedBy(changedByUser)
                .build();
        
        auditDAO.save(audit);
        log.info("Recorded direct modification for reservation {}", reservation.getId());
    }

    @Transactional(readOnly = true)
    public Page<ReservationAuditDTO> getHistory(Long reservationId, Pageable pageable) {
        log.debug("Fetching audit history for reservation {} (page: {}, size: {})", 
                  reservationId, pageable.getPageNumber(), pageable.getPageSize());
        
        // Validate reservation exists
        if (!reservationDAO.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        
        return auditDAO.findByReservationIdOrderByChangedAtDesc(reservationId, pageable)
                .map(auditMapper::toDTO);
    }

    /**
     * Get complete audit history for a reservation (non-paginated)
     * 
     * @param reservationId ID of the reservation
     * @return List of audit records
     */
    @Transactional(readOnly = true)
    public List<ReservationAuditDTO> getHistoryNonPaginated(Long reservationId) {
        log.debug("Fetching complete audit history for reservation {}", reservationId);
        
        // Validate reservation exists
        if (!reservationDAO.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        
        return auditDAO.findByReservationIdOrderByChangedAtDesc(reservationId, PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .map(auditMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get audit trail for all reservations at a restaurant
     * Useful for restaurant compliance and review purposes
     * 
     * @param restaurantId ID of the restaurant
     * @param pageable Pagination parameters
     * @return Page of audit records for the restaurant's reservations
     */
    @Transactional(readOnly = true)
    public Page<ReservationAuditDTO> getRestaurantAuditTrail(Long restaurantId, Pageable pageable) {
        log.debug("Fetching audit trail for restaurant {} (page: {}, size: {})", 
                  restaurantId, pageable.getPageNumber(), pageable.getPageSize());
        
        return auditDAO.findRestaurantAuditTrail(restaurantId, pageable)
                .map(auditMapper::toDTO);
    }

    /**
     * Get audit records for specific action type
     * 
     * @param reservationId ID of the reservation
     * @param action Type of action to filter by
     * @param pageable Pagination parameters
     * @return Page of audit records matching the action
     */
    @Transactional(readOnly = true)
    public Page<ReservationAuditDTO> getAuditByAction(Long reservationId, AuditAction action, Pageable pageable) {
        log.debug("Fetching audit records for reservation {} with action: {}", reservationId, action);
        
        // Validate reservation exists
        if (!reservationDAO.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        
        return auditDAO.findByReservationIdAndAction(reservationId, action, pageable)
                .map(auditMapper::toDTO);
    }

    /**
     * Get audit records created by specific user
     * 
     * @param reservationId ID of the reservation
     * @param userId ID of the user
     * @param pageable Pagination parameters
     * @return Page of audit records created by the user
     */
    @Transactional(readOnly = true)
    public Page<ReservationAuditDTO> getAuditByUser(Long reservationId, Long userId, Pageable pageable) {
        log.debug("Fetching audit records for reservation {} changed by user {}", reservationId, userId);
        
        // Validate reservation exists
        if (!reservationDAO.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        
        return auditDAO.findByReservationIdAndChangedByUserId(reservationId, userId, pageable)
                .map(auditMapper::toDTO);
    }

    /**
     * Get audit records within a date range
     * 
     * @param reservationId ID of the reservation
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of audit records within the range
     */
    @Transactional(readOnly = true)
    public Page<ReservationAuditDTO> getAuditByDateRange(Long reservationId, LocalDateTime startDate, 
                                                         LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching audit records for reservation {} between {} and {}", 
                  reservationId, startDate, endDate);
        
        // Validate reservation exists
        if (!reservationDAO.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        
        return auditDAO.findByReservationIdAndChangedAtBetween(reservationId, startDate, endDate, pageable)
                .map(auditMapper::toDTO);
    }

    /**
     * Count total audit records for a reservation
     * 
     * @param reservationId ID of the reservation
     * @return Count of audit records
     */
    @Transactional(readOnly = true)
    public long countAuditRecords(Long reservationId) {
        log.debug("Counting audit records for reservation {}", reservationId);
        
        // Validate reservation exists
        if (!reservationDAO.existsById(reservationId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        
        return auditDAO.countByReservationId(reservationId);
    }

    /**
     * Export audit trail to CSV format (future enhancement)
     * 
     * @param reservationId ID of the reservation
     * @return CSV formatted string
     */
    @Transactional(readOnly = true)
    public String exportToCSV(Long reservationId) {
        log.debug("Exporting audit trail for reservation {} to CSV", reservationId);
        
        List<ReservationAudit> audits = auditDAO.findByReservationIdOrderByChangedAtDesc(reservationId, 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        
        StringBuilder csv = new StringBuilder();
        csv.append("Action,ChangedAt,ChangedBy,Reason,Changes\n");
        
        for (ReservationAudit audit : audits) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                    audit.getAction(),
                    audit.getChangedAt(),
                    audit.getChangedBy() != null ? audit.getChangedBy().getUsername() : "SYSTEM",
                    audit.getChangeReason(),
                    escapeCSV(formatChangesForCSVFromJson(audit.getChangedFields()))));
        }
        
        return csv.toString();
    }

    /**
     * Helper method to format field changes from JSON for CSV export
     */
    private String formatChangesForCSVFromJson(String changedFieldsJson) {
        if (changedFieldsJson == null || changedFieldsJson.trim().isEmpty() || changedFieldsJson.equals("[]")) {
            return "";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<FieldChange> changes = mapper.readValue(changedFieldsJson, 
                    mapper.getTypeFactory().constructCollectionType(List.class, FieldChange.class));
            return changes.stream()
                    .map(fc -> String.format("%s: %s->%s", fc.getField(), fc.getOldValue(), fc.getNewValue()))
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            log.warn("Failed to deserialize changes from JSON", e);
            return "";
        }
    }

    /**
     * Escape CSV field values
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
