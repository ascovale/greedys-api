package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.application.common.config.CustomAuditingEntityListener;
import com.application.common.persistence.model.user.AbstractUser;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // -- Auditing fields -- //

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // Rimuoviamo @CreatedBy e @LastModifiedBy perché gestiti dal CustomAuditingEntityListener
    @Schema(hidden = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", updatable = false)
    private AbstractUser createdBy;

    @Column(name = "created_by_user_type", updatable = false)
    @Enumerated(EnumType.STRING)
    private UserType createdByUserType;

    @Schema(hidden = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_user_id")
    private AbstractUser modifiedBy;

    @Column(name = "modified_by_user_type")
    @Enumerated(EnumType.STRING)
    private UserType modifiedByUserType;

    @Schema(hidden = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private AbstractUser acceptedBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public enum UserType {
        CUSTOMER,
        ADMIN,
        RESTAURANT_USER,
        AGENCY_USER
    }

    public enum Status {
        NOT_ACCEPTED,
        ACCEPTED,
        REJECTED,
        SEATED,
        NO_SHOW,
        DELETED
    }

    /**
     * Combines date and slot start into a single LocalDateTime.
     */
    public LocalDateTime getReservationDateTime() {
        return LocalDateTime.of(date, slot.getStart());
    }

    /**
     * Checks if current time is after the restaurant's no-show limit.
     */
	// TODO: MOVE TO A SERVICE
    public boolean isAfterNoShowTimeLimit(LocalDateTime now) {
        return getReservationDateTime()
                .plusMinutes(restaurant.getNoShowTimeLimit())
                .isBefore(now.plusNanos(1));
    }
}
