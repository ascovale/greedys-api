package com.application.common.persistence.model.group;

import com.application.common.persistence.model.group.enums.BookerType;
import com.application.common.persistence.model.group.enums.EventCategory;
import com.application.common.persistence.model.group.enums.GroupBookingStatus;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.user.AbstractUser;
import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Prenotazione di gruppo - sia per Agency (B2B) che per Customer (B2C).
 * <p>
 * Supporta:
 * - Eventi di gruppo (matrimoni, cene aziendali, compleanni, etc.)
 * - Menù a prezzo fisso con conteggio PAX
 * - Gestione esigenze alimentari del gruppo
 * - Negoziazione prezzo tramite AgencyProposal
 * - Pagamenti con acconti
 */
@Entity
@Table(name = "group_bookings", indexes = {
    @Index(name = "idx_group_booking_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_group_booking_booker", columnList = "booker_id"),
    @Index(name = "idx_group_booking_agency", columnList = "agency_id"),
    @Index(name = "idx_group_booking_date", columnList = "event_date"),
    @Index(name = "idx_group_booking_status", columnList = "status"),
    @Index(name = "idx_group_booking_confirmation_code", columnList = "confirmation_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== RISTORANTE ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    // ==================== COLLEGAMENTO A RESERVATION ====================
    
    /**
     * Collegamento alla Reservation standard.
     * Quando un GroupBooking viene confermato, si crea una Reservation 
     * corrispondente per comparire nel calendario prenotazioni del ristorante.
     * Questo permette di avere una vista unificata di tutte le prenotazioni del giorno.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    // ==================== RICHIEDENTE ====================
    
    /**
     * Chi prenota: AGENCY o CUSTOMER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booker_type", nullable = false, length = 20)
    private BookerType bookerType;

    /**
     * Utente che ha creato la prenotazione (può essere un customer o un operatore agency)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id")
    private AbstractUser booker;

    /**
     * Agency (se bookerType = AGENCY)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private RestaurantAgency agency;

    // ==================== EVENTO ====================
    
    /**
     * Categoria dell'evento
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_category", length = 30)
    private EventCategory eventCategory;

    /**
     * Nome dell'evento (es. "Matrimonio Rossi-Bianchi", "Cena aziendale TechCorp")
     */
    @Column(name = "event_name", length = 200)
    private String eventName;

    /**
     * Descrizione o note aggiuntive sull'evento
     */
    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    /**
     * Durata stimata in minuti
     */
    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 180;

    // ==================== PARTECIPANTI ====================
    
    /**
     * Numero totale di partecipanti
     */
    @Column(name = "total_pax", nullable = false)
    private Integer totalPax;

    /**
     * Numero adulti
     */
    @Column(name = "adults_count")
    private Integer adultsCount;

    /**
     * Numero bambini
     */
    @Column(name = "children_count")
    @Builder.Default
    private Integer childrenCount = 0;

    /**
     * Esigenze alimentari del gruppo (vegetariani, celiaci, halal, etc.)
     */
    @OneToMany(mappedBy = "groupBooking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupBookingDietaryNeeds> dietaryNeeds = new ArrayList<>();

    // ==================== MENU ====================
    
    /**
     * Menù a prezzo fisso selezionato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_price_menu_id")
    private FixedPriceMenu fixedPriceMenu;

    /**
     * Proposta personalizzata (se applicabile)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private AgencyProposal proposal;

    /**
     * Selezioni specifiche del cliente per i piatti del menù.
     * JSON con struttura: { "courseId": [itemId1, itemId2], ... }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "menu_selections", columnDefinition = "jsonb")
    private JsonNode menuSelections;

    /**
     * Note speciali sul menù
     */
    @Column(name = "menu_notes", columnDefinition = "TEXT")
    private String menuNotes;

    // ==================== PRICING ====================
    
    /**
     * Prezzo per persona concordato
     */
    @Column(name = "price_per_person", precision = 10, scale = 2)
    private BigDecimal pricePerPerson;

    /**
     * Prezzo bambini (se diverso)
     */
    @Column(name = "children_price", precision = 10, scale = 2)
    private BigDecimal childrenPrice;

    /**
     * Supplementi totali
     */
    @Column(name = "total_supplements", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalSupplements = BigDecimal.ZERO;

    /**
     * Sconto applicato (valore assoluto)
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Sconto percentuale applicato
     */
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Importo totale calcolato
     */
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Valuta (default EUR)
     */
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "EUR";

    // ==================== PAGAMENTO ====================
    
    /**
     * Acconto richiesto
     */
    @Column(name = "deposit_required", precision = 10, scale = 2)
    private BigDecimal depositRequired;

    /**
     * Acconto pagato
     */
    @Column(name = "deposit_paid", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal depositPaid = BigDecimal.ZERO;

    /**
     * Data pagamento acconto
     */
    @Column(name = "deposit_paid_at")
    private LocalDateTime depositPaidAt;

    /**
     * Metodo di pagamento preferito
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /**
     * Flag: pagamento completato
     */
    @Column(name = "is_fully_paid")
    @Builder.Default
    private Boolean isFullyPaid = false;

    // ==================== CONTATTI ====================
    
    /**
     * Nome del referente per l'evento
     */
    @Column(name = "contact_name", length = 150)
    private String contactName;

    /**
     * Email del referente
     */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /**
     * Telefono del referente
     */
    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    // ==================== STATO ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private GroupBookingStatus status = GroupBookingStatus.INQUIRY;

    /**
     * Codice di conferma unico
     */
    @Column(name = "confirmation_code", unique = true, length = 20)
    private String confirmationCode;

    /**
     * Note interne del ristorante
     */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    /**
     * Motivo cancellazione (se cancellato)
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    // ==================== AUDIT ====================
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (confirmationCode == null) {
            confirmationCode = generateConfirmationCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Genera codice di conferma univoco
     */
    private String generateConfirmationCode() {
        return "GRP-" + System.currentTimeMillis() % 100000000L;
    }

    /**
     * Calcola il totale
     */
    public BigDecimal calculateTotal() {
        if (pricePerPerson == null || totalPax == null) {
            return BigDecimal.ZERO;
        }
        
        int adults = adultsCount != null ? adultsCount : totalPax;
        int children = childrenCount != null ? childrenCount : 0;
        
        BigDecimal adultsTotal = pricePerPerson.multiply(BigDecimal.valueOf(adults));
        BigDecimal childrenTotal = BigDecimal.ZERO;
        
        if (children > 0 && childrenPrice != null) {
            childrenTotal = childrenPrice.multiply(BigDecimal.valueOf(children));
        } else if (children > 0) {
            // Default: children 50% of adult price
            childrenTotal = pricePerPerson.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(children));
        }
        
        BigDecimal subtotal = adultsTotal.add(childrenTotal);
        
        // Add supplements
        if (totalSupplements != null) {
            subtotal = subtotal.add(totalSupplements);
        }
        
        // Apply discount
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            subtotal = subtotal.subtract(discountAmount);
        } else if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = subtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
            subtotal = subtotal.subtract(discount);
        }
        
        return subtotal.max(BigDecimal.ZERO);
    }

    /**
     * Verifica se l'acconto è stato pagato completamente
     */
    public boolean isDepositComplete() {
        if (depositRequired == null || depositRequired.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return depositPaid != null && depositPaid.compareTo(depositRequired) >= 0;
    }

    /**
     * Verifica se è una prenotazione Agency
     */
    public boolean isAgencyBooking() {
        return BookerType.AGENCY.equals(bookerType) && agency != null;
    }

    /**
     * Ottiene il LocalDateTime completo dell'evento
     */
    public LocalDateTime getEventDateTime() {
        if (eventDate == null || eventTime == null) {
            return null;
        }
        return LocalDateTime.of(eventDate, eventTime);
    }

    /**
     * Verifica se la prenotazione può essere confermata
     */
    public boolean canConfirm() {
        return status == GroupBookingStatus.PENDING 
            || status == GroupBookingStatus.INQUIRY;
    }

    /**
     * Verifica se la prenotazione può essere cancellata
     */
    public boolean canCancel() {
        return status != GroupBookingStatus.CANCELLED 
            && status != GroupBookingStatus.COMPLETED
            && status != GroupBookingStatus.NO_SHOW;
    }

    /**
     * Aggiunge esigenze alimentari
     */
    public void addDietaryNeed(GroupBookingDietaryNeeds need) {
        if (dietaryNeeds == null) {
            dietaryNeeds = new ArrayList<>();
        }
        need.setGroupBooking(this);
        dietaryNeeds.add(need);
    }

    /**
     * Calcola il totale PAX con esigenze speciali
     */
    public int getTotalSpecialDietaryPax() {
        if (dietaryNeeds == null || dietaryNeeds.isEmpty()) {
            return 0;
        }
        return dietaryNeeds.stream()
            .mapToInt(GroupBookingDietaryNeeds::getPaxCount)
            .sum();
    }
}
