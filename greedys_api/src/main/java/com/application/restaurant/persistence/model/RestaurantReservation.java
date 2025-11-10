package com.application.restaurant.persistence.model;

import java.time.Instant;
import java.time.LocalDateTime;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity linking restaurants with customer reservations and agenda management
 * Tracks customers in restaurant's contact agenda regardless of account status
 */
@Entity
@Table(name = "restaurant_reservation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = true)
    private Reservation reservation; // Can be null for agenda-only entries

    @Column(name = "first_interaction", nullable = false)
    private Instant firstInteraction; // When customer was first added to restaurant agenda

    @Column(name = "last_interaction", nullable = true)
    private Instant lastInteraction; // Last reservation or contact

    @Column(name = "total_reservations", nullable = false)
    @Builder.Default
    private Integer totalReservations = 0;

    @Column(name = "total_guests", nullable = false)
    @Builder.Default
    private Integer totalGuests = 0;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false; // Restaurant can mark as favorite customer

    @Column(name = "is_blacklisted", nullable = false)
    @Builder.Default
    private Boolean isBlacklisted = false; // Restaurant can blacklist customer

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes; // Restaurant's private notes about customer

    @Column(name = "preferences", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode preferences; // Customer preferences specific to this restaurant

    @Column(name = "contact_source") // How customer was added: "reservation", "phone", "manual", "import"
    private String contactSource;

    @Column(name = "last_reservation_date")
    private LocalDateTime lastReservationDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Update reservation statistics
     */
    public void incrementReservationStats(Integer guestCount) {
        this.totalReservations++;
        if (guestCount != null && guestCount > 0) {
            this.totalGuests += guestCount;
        }
        this.lastInteraction = Instant.now();
        this.lastReservationDate = LocalDateTime.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Update last interaction timestamp
     */
    public void updateLastInteraction() {
        this.lastInteraction = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Check if customer is a regular (multiple reservations)
     */
    public boolean isRegularCustomer() {
        return totalReservations >= 3;
    }

    /**
     * Check if customer is new (first interaction within last 30 days)
     */
    public boolean isNewCustomer() {
        if (firstInteraction == null) return false;
        return firstInteraction.isAfter(Instant.now().minusSeconds(30 * 24 * 60 * 60)); // 30 days
    }

    /**
     * Get average guests per reservation
     */
    public double getAverageGuestsPerReservation() {
        if (totalReservations == 0) return 0.0;
        return (double) totalGuests / totalReservations;
    }

    /**
     * Check if customer has special status
     */
    public boolean hasSpecialStatus() {
        return isFavorite || isBlacklisted;
    }

    /**
     * Get customer status for display
     */
    public String getCustomerStatus() {
        if (isBlacklisted) return "BLACKLISTED";
        if (isFavorite) return "FAVORITE";
        if (isRegularCustomer()) return "REGULAR";
        if (isNewCustomer()) return "NEW";
        return "STANDARD";
    }
}