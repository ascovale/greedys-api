package com.application.common.persistence.model.group;

import com.application.common.persistence.model.group.enums.ProposalStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Revisione/versione di una proposta.
 * <p>
 * Ogni modifica a una AgencyProposal crea una nuova revisione per
 * mantenere l'audit trail completo della negoziazione.
 */
@Entity
@Table(name = "proposal_revisions", indexes = {
    @Index(name = "idx_revision_proposal", columnList = "proposal_id"),
    @Index(name = "idx_revision_version", columnList = "version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposalRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private AgencyProposal proposal;

    /**
     * Numero di versione
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Chi ha creato questa revisione: RESTAURANT o AGENCY
     */
    @Column(name = "modified_by_type", nullable = false, length = 20)
    private String modifiedByType;

    /**
     * ID dell'utente che ha fatto la modifica
     */
    @Column(name = "modified_by_user_id")
    private Long modifiedByUserId;

    /**
     * Nome dell'utente che ha fatto la modifica
     */
    @Column(name = "modified_by_name", length = 150)
    private String modifiedByName;

    // ==================== SNAPSHOT DEI DATI ====================

    /**
     * Stato della proposta in questa versione
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private ProposalStatus status;

    /**
     * Prezzo proposto in questa versione
     */
    @Column(name = "proposed_price", precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    /**
     * Prezzo bambini in questa versione
     */
    @Column(name = "children_price", precision = 10, scale = 2)
    private BigDecimal childrenPrice;

    /**
     * Sconto percentuale in questa versione
     */
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Contenuto custom in questa versione (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_content", columnDefinition = "jsonb")
    private JsonNode customContent;

    /**
     * Descrizione in questa versione
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Termini e condizioni in questa versione
     */
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    // ==================== METADATI MODIFICA ====================

    /**
     * Tipo di modifica
     */
    @Column(name = "change_type", length = 50)
    private String changeType;

    /**
     * Descrizione della modifica
     */
    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    /**
     * Campi modificati (JSON array)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private JsonNode changedFields;

    /**
     * Nota/commento per la controparte
     */
    @Column(name = "note_to_counterparty", columnDefinition = "TEXT")
    private String noteToCounterparty;

    // ==================== TIMESTAMPS ====================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se è stata fatta dal ristorante
     */
    public boolean isByRestaurant() {
        return "RESTAURANT".equals(modifiedByType);
    }

    /**
     * Verifica se è stata fatta dall'agency
     */
    public boolean isByAgency() {
        return "AGENCY".equals(modifiedByType);
    }
}
