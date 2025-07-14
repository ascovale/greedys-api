package com.application.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.user.AbstractUser;

/**
 * Refactored Reservation entity with centralized auditing and reduced duplication.
 */
@Entity
@Table(name = "reservation")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurant")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_customer")
    private Customer customer;

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
    private com.application.persistence.model.restaurant.Table table;

    // -- Auditing fields -- //

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", updatable = false)
    private AbstractUser createdBy;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_user_id")
    private AbstractUser modifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private AbstractUser acceptedBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
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
