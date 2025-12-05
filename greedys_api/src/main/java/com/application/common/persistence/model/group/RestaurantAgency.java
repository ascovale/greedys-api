package com.application.common.persistence.model.group;

import com.application.agency.persistence.model.Agency;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Relazione B2B tra Ristorante e Agenzia.
 * <p>
 * Simile a RestaurantReservation ma per il contesto Agency.
 * Definisce i termini commerciali tra un ristorante e un'agenzia.
 */
@Entity
@Table(name = "restaurant_agency", indexes = {
    @Index(name = "idx_restaurant_agency_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_restaurant_agency_agency", columnList = "agency_id"),
    @Index(name = "idx_restaurant_agency_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_restaurant_agency", columnNames = {"restaurant_id", "agency_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantAgency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    // ==================== STATUS ====================

    /**
     * Relazione attiva
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Relazione approvata dal ristorante
     */
    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = false;

    /**
     * Agency verificata/trusted
     */
    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean isTrusted = false;

    // ==================== TERMINI COMMERCIALI ====================

    /**
     * Sconto percentuale default per l'agency
     */
    @Column(name = "default_discount_percentage", precision = 5, scale = 2)
    private BigDecimal defaultDiscountPercentage;

    /**
     * Commissione percentuale per l'agency
     */
    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    /**
     * Numero minimo di PAX per prenotazioni
     */
    @Column(name = "min_pax")
    private Integer minPax;

    /**
     * Numero massimo di PAX per prenotazioni
     */
    @Column(name = "max_pax")
    private Integer maxPax;

    /**
     * Acconto richiesto (percentuale)
     */
    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    private BigDecimal depositPercentage;

    /**
     * Giorni di preavviso richiesti per prenotazioni
     */
    @Column(name = "advance_booking_days")
    private Integer advanceBookingDays;

    /**
     * Termini di pagamento (giorni)
     */
    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    // ==================== NOTE ====================

    /**
     * Note visibili all'agency
     */
    @Column(name = "notes_for_agency", columnDefinition = "TEXT")
    private String notesForAgency;

    /**
     * Note interne del ristorante
     */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // ==================== CONTATTI ====================

    /**
     * Contatto dedicato del ristorante per questa agency
     */
    @Column(name = "restaurant_contact_name", length = 150)
    private String restaurantContactName;

    @Column(name = "restaurant_contact_email", length = 255)
    private String restaurantContactEmail;

    @Column(name = "restaurant_contact_phone", length = 30)
    private String restaurantContactPhone;

    /**
     * Contatto dedicato dell'agency
     */
    @Column(name = "agency_contact_name", length = 150)
    private String agencyContactName;

    @Column(name = "agency_contact_email", length = 255)
    private String agencyContactEmail;

    @Column(name = "agency_contact_phone", length = 30)
    private String agencyContactPhone;

    // ==================== STATISTICHE ====================

    /**
     * Numero totale di prenotazioni
     */
    @Column(name = "total_bookings")
    @Builder.Default
    private Integer totalBookings = 0;

    /**
     * Valore totale delle prenotazioni
     */
    @Column(name = "total_revenue", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    /**
     * Data ultima prenotazione
     */
    @Column(name = "last_booking_date")
    private LocalDateTime lastBookingDate;

    // ==================== AUDIT ====================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Verifica se l'agency può fare prenotazioni
     */
    public boolean canBook() {
        return Boolean.TRUE.equals(isActive) && Boolean.TRUE.equals(isApproved);
    }

    /**
     * Verifica se il numero di PAX è valido
     */
    public boolean isValidPax(int pax) {
        if (minPax != null && pax < minPax) {
            return false;
        }
        if (maxPax != null && pax > maxPax) {
            return false;
        }
        return true;
    }

    /**
     * Incrementa contatore prenotazioni
     */
    public void incrementBookings(BigDecimal amount) {
        this.totalBookings = (this.totalBookings != null ? this.totalBookings : 0) + 1;
        this.totalRevenue = (this.totalRevenue != null ? this.totalRevenue : BigDecimal.ZERO).add(amount);
        this.lastBookingDate = LocalDateTime.now();
    }
}
