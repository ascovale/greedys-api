package com.application.common.persistence.model.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ SUPPORT FAQ ENTITY
 * 
 * FAQ per il sistema di supporto bot.
 * Usato per matching automatico e suggerimenti.
 * 
 * FEATURES:
 * - Domande e risposte per categoria
 * - Keywords per matching
 * - Target user type (Customer, Restaurant, Agency)
 * - Ordering per priorità
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "support_faq", indexes = {
    @Index(name = "idx_faq_category", columnList = "category"),
    @Index(name = "idx_faq_target", columnList = "target_user_type"),
    @Index(name = "idx_faq_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportFAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Domanda FAQ
     */
    @Column(name = "question", nullable = false, length = 500)
    private String question;

    /**
     * Risposta FAQ
     */
    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    /**
     * Categoria
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private TicketCategory category;

    /**
     * Tipo utente target (a chi mostrare questa FAQ)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_user_type", nullable = false, length = 20)
    private RequesterType targetUserType;

    /**
     * Keywords per matching automatico
     */
    @ElementCollection
    @CollectionTable(name = "support_faq_keywords", joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "keyword", length = 100)
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    /**
     * Ordine di visualizzazione
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Se attiva
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Conteggio utilizzi (quante volte è stata mostrata/usata)
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * Conteggio "utile" (feedback positivo)
     */
    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    /**
     * Conteggio "non utile" (feedback negativo)
     */
    @Column(name = "not_helpful_count")
    @Builder.Default
    private Integer notHelpfulCount = 0;

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Data ultimo aggiornamento
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (usageCount == null) {
            usageCount = 0;
        }
        if (helpfulCount == null) {
            helpfulCount = 0;
        }
        if (notHelpfulCount == null) {
            notHelpfulCount = 0;
        }
        if (keywords == null) {
            keywords = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Incrementa contatore utilizzo
     */
    public void incrementUsage() {
        this.usageCount++;
    }

    /**
     * Registra feedback positivo
     */
    public void markHelpful() {
        this.helpfulCount++;
    }

    /**
     * Registra feedback negativo
     */
    public void markNotHelpful() {
        this.notHelpfulCount++;
    }

    /**
     * Calcola percentuale di utilità
     */
    public double getHelpfulPercentage() {
        int total = helpfulCount + notHelpfulCount;
        if (total == 0) return 0.0;
        return (helpfulCount * 100.0) / total;
    }

    /**
     * Attiva la FAQ
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Disattiva la FAQ
     */
    public void deactivate() {
        this.isActive = false;
    }
}
