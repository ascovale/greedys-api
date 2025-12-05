package com.application.common.persistence.model.event;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * ⭐ EVENT RSVP ENTITY
 * 
 * Rappresenta la risposta (RSVP) di un utente a un evento.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "event_rsvps", 
    indexes = {
        @Index(name = "idx_rsvp_event", columnList = "event_id"),
        @Index(name = "idx_rsvp_user", columnList = "user_id"),
        @Index(name = "idx_rsvp_status", columnList = "status"),
        @Index(name = "idx_rsvp_created", columnList = "created_at DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_user_rsvp", columnNames = {"event_id", "user_id"})
    }
)
public class EventRSVP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Evento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private RestaurantEvent event;

    // ==================== USER ====================
    
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType;

    // ==================== RSVP STATUS ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RSVPStatus status = RSVPStatus.INTERESTED;

    @Column(name = "guests_count", nullable = false)
    private Integer guestsCount = 1;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    // ==================== PAYMENT ====================
    
    @Column(name = "has_paid", nullable = false)
    private Boolean hasPaid = false;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "paid_at")
    private Instant paidAt;

    // ==================== CHECK-IN ====================
    
    @Column(name = "checked_in", nullable = false)
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    // ==================== CONTACT ====================
    
    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    // ==================== NOTES ====================
    
    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Column(name = "internal_notes", length = 500)
    private String internalNotes;

    /**
     * Note generiche dell'utente
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Data conferma RSVP
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * Data promozione da waitlist
     */
    @Column(name = "promoted_from_waitlist_at")
    private Instant promotedFromWaitlistAt;

    // ==================== NOTIFICATIONS ====================
    
    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // ==================== GETTERS/SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RestaurantEvent getEvent() {
        return event;
    }

    public void setEvent(RestaurantEvent event) {
        this.event = event;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public RSVPStatus getStatus() {
        return status;
    }

    public void setStatus(RSVPStatus status) {
        this.status = status;
    }

    public Integer getGuestsCount() {
        return guestsCount;
    }

    public void setGuestsCount(Integer guestsCount) {
        this.guestsCount = guestsCount;
    }

    public Integer getWaitlistPosition() {
        return waitlistPosition;
    }

    public void setWaitlistPosition(Integer waitlistPosition) {
        this.waitlistPosition = waitlistPosition;
    }

    public Boolean getHasPaid() {
        return hasPaid;
    }

    public void setHasPaid(Boolean hasPaid) {
        this.hasPaid = hasPaid;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Boolean getCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(Boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Long getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(Long checkedInBy) {
        this.checkedInBy = checkedInBy;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public Boolean getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(Instant reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getPromotedFromWaitlistAt() {
        return promotedFromWaitlistAt;
    }

    public void setPromotedFromWaitlistAt(Instant promotedFromWaitlistAt) {
        this.promotedFromWaitlistAt = promotedFromWaitlistAt;
    }

    // ==================== HELPER METHODS ====================

    public boolean isGoing() {
        return status == RSVPStatus.GOING || status == RSVPStatus.CONFIRMED;
    }

    public boolean isOnWaitlist() {
        return status == RSVPStatus.WAITLIST;
    }

    public void confirm() {
        this.status = RSVPStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = RSVPStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void checkIn(Long byUserId) {
        this.checkedIn = true;
        this.checkedInAt = Instant.now();
        this.checkedInBy = byUserId;
    }

    public void markPaid(BigDecimal amount, String reference) {
        this.hasPaid = true;
        this.amountPaid = amount;
        this.paymentReference = reference;
        this.paidAt = Instant.now();
    }

    public void markReminderSent() {
        this.reminderSent = true;
        this.reminderSentAt = Instant.now();
    }
}
