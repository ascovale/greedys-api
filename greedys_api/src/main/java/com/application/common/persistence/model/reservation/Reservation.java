package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.application.common.config.CustomAuditingEntityListener;
import com.application.common.persistence.model.user.AbstractUser;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reservation entity for managing restaurant reservations.
 * This class is hidden from OpenAPI documentation to prevent exposure of internal JPA model structure.
 * Internal model - use ReservationDTO for API responses.
 */
@Hidden
@Entity
@Table(name = "reservation")
@EntityListeners(CustomAuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_restaurant")
    private Restaurant restaurant;

    private String userName;

    @Column(name = "r_date", nullable = false)
    private LocalDate date;

    /**
     * Reference to the Service (current configuration).
     * Use snapshot fields for booking-time values.
     * 
     * NULLABLE: Per prenotazioni di gruppo o fuori orario, il service può essere null.
     * In questo caso, usare i campi snapshot (bookedServiceName) per identificare il contesto.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "service_id", nullable = true)
    private Service service;

    /**
     * Tipo di prenotazione speciale. NULL = prenotazione standard.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "special_booking_type", length = 30)
    private SpecialBookingType specialBookingType;

    @Column(name = "reservation_datetime", nullable = false)
    private LocalDateTime reservationDateTime;

    // ─────────────────────────────────────────────────────────────────────────────
    // SNAPSHOT FIELDS - Values captured at booking time (contract terms)
    // These preserve the original booking conditions even if Service changes later
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Service name at time of booking.
     * Preserved even if service is renamed later.
     */
    @Column(name = "booked_service_name", length = 100)
    private String bookedServiceName;

    /**
     * Slot duration in minutes at time of booking.
     * E.g., 30, 60, 90, 120 minutes.
     */
    @Column(name = "booked_slot_duration")
    private Integer bookedSlotDuration;

    /**
     * Opening time at time of booking.
     * E.g., 12:00 for lunch service.
     */
    @Column(name = "booked_opening_time")
    private LocalTime bookedOpeningTime;

    /**
     * Closing time at time of booking.
     * E.g., 15:00 for lunch service.
     */
    @Column(name = "booked_closing_time")
    private LocalTime bookedClosingTime;

    @Column(nullable = false)
    private Integer pax;

    @Column(nullable = false)
	@Builder.Default
    private Integer kids = 0;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
	@Builder.Default
    private Status status = Status.NOT_ACCEPTED;

    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private com.application.restaurant.persistence.model.Table table;

    @Column(name = "table_number")
    private Integer tableNumber;

    @Column(length = 500, name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "reservationAsGuest", cascade = CascadeType.REMOVE)
    @Builder.Default
    private Set<Customer> dinnerGuests = new HashSet<>();

    // -- Auditing fields -- //

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", updatable = false)
    private AbstractUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_user_id")
    private AbstractUser modifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private AbstractUser acceptedBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    // Auditing fields moved to AbstractUser with JOINED inheritance
    // createdBy, modifiedBy, acceptedBy now reference AbstractUser directly
    // (no more need for createdByUserType/modifiedByUserType - use instanceof to determine type)

    public enum Status {
        NOT_ACCEPTED,
        ACCEPTED,
        REJECTED,
        SEATED,
        NO_SHOW,
        DELETED
    }

    /**
     * Returns the reservation date and time (explicit datetime, no slot dependency).
     */
    public LocalDateTime getReservationDateTime() {
        return reservationDateTime;
    }

    /**
     * Returns the time portion of the reservation datetime.
     */
    public LocalTime getReservationTime() {
        return reservationDateTime != null ? reservationDateTime.toLocalTime() : null;
    }

    /**
     * Returns the date portion of the reservation datetime.
     */
    public LocalDate getReservationDate() {
        return reservationDateTime != null ? reservationDateTime.toLocalDate() : null;
    }

    /**
     * Checks if current time is after the restaurant's no-show limit.
     */
	// TODO: MOVE TO A SERVICE
    public boolean isAfterNoShowTimeLimit(LocalDateTime now) {
        return reservationDateTime
                .plusMinutes(restaurant.getNoShowTimeLimit())
                .isBefore(now.plusNanos(1));
    }

    /**
     * Adds a customer as a dinner guest for this reservation.
     * 
     * @param guest The customer to add as a dinner guest
     * @return true if the guest was added, false if already present
     */
    public boolean addDinnerGuest(Customer guest) {
        if (guest != null && dinnerGuests.add(guest)) {
            guest.setReservationAsGuest(this);
            return true;
        }
        return false;
    }

    /**
     * Removes a customer from the dinner guests.
     * 
     * @param guest The customer to remove
     * @return true if the guest was removed, false if not present
     */
    public boolean removeDinnerGuest(Customer guest) {
        if (guest != null && dinnerGuests.remove(guest)) {
            guest.setReservationAsGuest(null);
            return true;
        }
        return false;
    }

    /**
     * Clears all dinner guests from this reservation.
     */
    public void clearDinnerGuests() {
        for (Customer guest : new HashSet<>(dinnerGuests)) {
            removeDinnerGuest(guest);
        }
    }

    /**
     * Returns the total number of dinner participants (including the main customer and guests).
     * 
     * @return Total participants count
     */
    public int getTotalParticipants() {
        return 1 + (dinnerGuests != null ? dinnerGuests.size() : 0); // 1 per il customer principale
    }
}
