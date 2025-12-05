package com.application.common.persistence.model.group;

import com.application.agency.persistence.model.Agency;
import com.application.common.persistence.model.group.enums.ProposalStatus;
import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Proposta personalizzata per un'Agency.
 * <p>
 * Una proposta può essere:
 * - EXCLUSIVE: visibile solo a questa agency
 * - Basata su un FixedPriceMenu esistente con modifiche
 * - Completamente custom
 * <p>
 * Supporta negoziazione bidirezionale con audit trail completo.
 */
@Entity
@Table(name = "agency_proposals", indexes = {
    @Index(name = "idx_proposal_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_proposal_agency", columnList = "agency_id"),
    @Index(name = "idx_proposal_status", columnList = "status"),
    @Index(name = "idx_proposal_valid_until", columnList = "valid_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== PARTI COINVOLTE ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    /**
     * Relazione B2B (opzionale, per accedere ai termini commerciali)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_agency_id")
    private RestaurantAgency restaurantAgency;

    // ==================== DETTAGLI PROPOSTA ====================

    /**
     * Titolo della proposta
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Descrizione dettagliata
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Codice univoco della proposta
     */
    @Column(name = "proposal_code", unique = true, length = 30)
    private String proposalCode;

    /**
     * Menù base su cui è costruita la proposta (opzionale)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_menu_id")
    private FixedPriceMenu baseMenu;

    /**
     * Contenuto personalizzato della proposta (JSON).
     * Può contenere: courses, items, modifiche al menu base, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_content", columnDefinition = "jsonb")
    private JsonNode customContent;

    // ==================== PRICING ====================

    /**
     * Prezzo per persona proposto
     */
    @Column(name = "proposed_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal proposedPrice;

    /**
     * Prezzo originale (se diverso dal proposto)
     */
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Sconto percentuale applicato
     */
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Prezzo bambini
     */
    @Column(name = "children_price", precision = 10, scale = 2)
    private BigDecimal childrenPrice;

    /**
     * Numero minimo PAX
     */
    @Column(name = "min_pax")
    private Integer minPax;

    /**
     * Numero massimo PAX
     */
    @Column(name = "max_pax")
    private Integer maxPax;

    // ==================== VALIDITÀ ====================

    /**
     * Data inizio validità
     */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /**
     * Data fine validità
     */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    /**
     * Proposta esclusiva (non visibile ad altre agency)
     */
    @Column(name = "is_exclusive")
    @Builder.Default
    private Boolean isExclusive = true;

    // ==================== STATO ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.DRAFT;

    /**
     * Versione corrente (incrementa ad ogni modifica)
     */
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    /**
     * Chi ha fatto l'ultima modifica: RESTAURANT o AGENCY
     */
    @Column(name = "last_modified_by_type", length = 20)
    private String lastModifiedByType;

    // ==================== AUDIT TRAIL ====================

    /**
     * Storico delle revisioni
     */
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version DESC")
    @Builder.Default
    private List<ProposalRevision> revisions = new ArrayList<>();

    // ==================== TERMINI E CONDIZIONI ====================

    /**
     * Acconto richiesto (percentuale)
     */
    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    private BigDecimal depositPercentage;

    /**
     * Giorni di preavviso per cancellazione gratuita
     */
    @Column(name = "cancellation_days")
    private Integer cancellationDays;

    /**
     * Termini e condizioni specifici
     */
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // ==================== NOTE ====================

    /**
     * Note per l'agency
     */
    @Column(name = "notes_for_agency", columnDefinition = "TEXT")
    private String notesForAgency;

    /**
     * Note interne del ristorante
     */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // ==================== TIMESTAMPS ====================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (proposalCode == null) {
            proposalCode = generateProposalCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY METHODS ====================

    private String generateProposalCode() {
        return "PRP-" + System.currentTimeMillis() % 100000000L;
    }

    /**
     * Verifica se la proposta è ancora valida
     */
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        if (validFrom != null && today.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && today.isAfter(validUntil)) {
            return false;
        }
        return status == ProposalStatus.ACTIVE || status == ProposalStatus.SENT;
    }

    /**
     * Verifica se può essere modificata
     */
    public boolean canEdit() {
        return status == ProposalStatus.DRAFT 
            || status == ProposalStatus.SENT 
            || status == ProposalStatus.COUNTER_PROPOSAL;
    }

    /**
     * Verifica se può essere accettata
     */
    public boolean canAccept() {
        return status == ProposalStatus.SENT || status == ProposalStatus.COUNTER_PROPOSAL;
    }

    /**
     * Calcola lo sconto applicato
     */
    public BigDecimal calculateDiscount() {
        if (originalPrice == null || proposedPrice == null) {
            return BigDecimal.ZERO;
        }
        if (originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return originalPrice.subtract(proposedPrice)
            .divide(originalPrice, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Aggiunge una revisione allo storico
     */
    public void addRevision(ProposalRevision revision) {
        if (revisions == null) {
            revisions = new ArrayList<>();
        }
        revision.setProposal(this);
        revision.setVersion(this.version);
        revisions.add(revision);
        this.version++;
    }

    /**
     * Verifica se il PAX è valido per questa proposta
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
}
