package com.application.common.persistence.model.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * ⭐ RESTAURANT EVENT ENTITY
 * 
 * Rappresenta un evento organizzato da un ristorante.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "restaurant_events", indexes = {
    @Index(name = "idx_event_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_event_date", columnList = "event_date"),
    @Index(name = "idx_event_status", columnList = "status"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_event_active", columnList = "is_active, is_deleted")
})
public class RestaurantEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== RESTAURANT ====================
    
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    // ==================== EVENT INFO ====================
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private RestaurantEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    // ==================== DATE/TIME ====================
    
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * Durata stimata in minuti
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // ==================== CAPACITY ====================
    
    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "current_attendees", nullable = false)
    private Integer currentAttendees = 0;

    @Column(name = "min_attendees")
    private Integer minAttendees;

    @Column(name = "waitlist_capacity")
    private Integer waitlistCapacity;

    @Column(name = "waitlist_count", nullable = false)
    private Integer waitlistCount = 0;

    // ==================== PRICING ====================
    
    @Column(name = "is_free", nullable = false)
    private Boolean isFree = true;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "early_bird_price", precision = 10, scale = 2)
    private BigDecimal earlyBirdPrice;

    @Column(name = "early_bird_deadline")
    private Instant earlyBirdDeadline;

    @Column(name = "currency", length = 3)
    private String currency = "EUR";

    // ==================== MEDIA ====================
    
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventMedia> media = new ArrayList<>();

    // ==================== RSVPs ====================
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventRSVP> rsvps = new ArrayList<>();

    // ==================== SETTINGS ====================
    
    @Column(name = "requires_reservation", nullable = false)
    private Boolean requiresReservation = false;

    @Column(name = "requires_payment", nullable = false)
    private Boolean requiresPayment = false;

    @Column(name = "allows_waitlist", nullable = false)
    private Boolean allowsWaitlist = true;

    @Column(name = "show_attendee_count", nullable = false)
    private Boolean showAttendeeCount = true;

    @Column(name = "registration_deadline")
    private Instant registrationDeadline;

    @Column(name = "cancellation_deadline")
    private Instant cancellationDeadline;

    // ==================== SOCIAL ====================
    
    @Column(name = "social_post_id")
    private Long socialPostId;

    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    @Column(name = "shares_count", nullable = false)
    private Integer sharesCount = 0;

    // ==================== STATUS FLAGS ====================
    
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern", length = 100)
    private String recurrencePattern;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // ==================== CONTACT ====================
    
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    // ==================== TIMESTAMPS ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ==================== GETTERS/SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public RestaurantEventType getEventType() {
        return eventType;
    }

    public void setEventType(RestaurantEventType eventType) {
        this.eventType = eventType;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getCurrentAttendees() {
        return currentAttendees;
    }

    public void setCurrentAttendees(Integer currentAttendees) {
        this.currentAttendees = currentAttendees;
    }

    public Integer getMinAttendees() {
        return minAttendees;
    }

    public void setMinAttendees(Integer minAttendees) {
        this.minAttendees = minAttendees;
    }

    public Integer getWaitlistCapacity() {
        return waitlistCapacity;
    }

    public void setWaitlistCapacity(Integer waitlistCapacity) {
        this.waitlistCapacity = waitlistCapacity;
    }

    public Integer getWaitlistCount() {
        return waitlistCount;
    }

    public void setWaitlistCount(Integer waitlistCount) {
        this.waitlistCount = waitlistCount;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getEarlyBirdPrice() {
        return earlyBirdPrice;
    }

    public void setEarlyBirdPrice(BigDecimal earlyBirdPrice) {
        this.earlyBirdPrice = earlyBirdPrice;
    }

    public Instant getEarlyBirdDeadline() {
        return earlyBirdDeadline;
    }

    public void setEarlyBirdDeadline(Instant earlyBirdDeadline) {
        this.earlyBirdDeadline = earlyBirdDeadline;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<EventMedia> getMedia() {
        return media;
    }

    public void setMedia(List<EventMedia> media) {
        this.media = media;
    }

    public List<EventRSVP> getRsvps() {
        return rsvps;
    }

    public void setRsvps(List<EventRSVP> rsvps) {
        this.rsvps = rsvps;
    }

    public Boolean getRequiresReservation() {
        return requiresReservation;
    }

    public void setRequiresReservation(Boolean requiresReservation) {
        this.requiresReservation = requiresReservation;
    }

    public Boolean getRequiresPayment() {
        return requiresPayment;
    }

    public void setRequiresPayment(Boolean requiresPayment) {
        this.requiresPayment = requiresPayment;
    }

    public Boolean getAllowsWaitlist() {
        return allowsWaitlist;
    }

    public void setAllowsWaitlist(Boolean allowsWaitlist) {
        this.allowsWaitlist = allowsWaitlist;
    }

    public Boolean getShowAttendeeCount() {
        return showAttendeeCount;
    }

    public void setShowAttendeeCount(Boolean showAttendeeCount) {
        this.showAttendeeCount = showAttendeeCount;
    }

    public Instant getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(Instant registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public Instant getCancellationDeadline() {
        return cancellationDeadline;
    }

    public void setCancellationDeadline(Instant cancellationDeadline) {
        this.cancellationDeadline = cancellationDeadline;
    }

    public Long getSocialPostId() {
        return socialPostId;
    }

    public void setSocialPostId(Long socialPostId) {
        this.socialPostId = socialPostId;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Integer getSharesCount() {
        return sharesCount;
    }

    public void setSharesCount(Integer sharesCount) {
        this.sharesCount = sharesCount;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    // ==================== HELPER METHODS ====================

    public boolean hasAvailableSpots() {
        return maxCapacity == null || currentAttendees < maxCapacity;
    }

    public int getAvailableSpots() {
        if (maxCapacity == null) return Integer.MAX_VALUE;
        return Math.max(0, maxCapacity - currentAttendees);
    }

    public boolean isSoldOut() {
        return status == EventStatus.SOLD_OUT || (maxCapacity != null && currentAttendees >= maxCapacity);
    }

    public boolean isRegistrationOpen() {
        if (registrationDeadline == null) return true;
        return Instant.now().isBefore(registrationDeadline);
    }

    public void incrementAttendees() {
        this.currentAttendees++;
        if (maxCapacity != null && currentAttendees >= maxCapacity) {
            this.status = EventStatus.SOLD_OUT;
        }
    }

    public void decrementAttendees() {
        if (this.currentAttendees > 0) {
            this.currentAttendees--;
            if (status == EventStatus.SOLD_OUT && hasAvailableSpots()) {
                this.status = EventStatus.PUBLISHED;
            }
        }
    }

    public void publish() {
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void cancel() {
        this.status = EventStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void softDelete() {
        this.isDeleted = true;
        this.isActive = false;
        this.deletedAt = Instant.now();
    }

    public void incrementViews() {
        this.viewsCount++;
    }

    public void incrementShares() {
        this.sharesCount++;
    }
}
