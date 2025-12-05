package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.RankingPeriod;
import com.application.challenge.persistence.model.enums.RankingScope;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ranking - Classifica dei ristoranti.
 * <p>
 * Rappresenta una classifica per zona/città/regione e per periodo temporale.
 * Può essere filtrata per tipo di cucina.
 */
@Entity
@Table(name = "ranking", indexes = {
    @Index(name = "idx_ranking_scope", columnList = "scope, city, zone"),
    @Index(name = "idx_ranking_active", columnList = "is_active, period_end"),
    @Index(name = "idx_ranking_cuisine", columnList = "cuisine_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome della classifica (es: "Top Pizza Roma Centro Q4 2025")
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Descrizione opzionale
     */
    @Column(length = 500)
    private String description;

    /**
     * Ambito geografico della classifica
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RankingScope scope;

    /**
     * Periodo temporale della classifica
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RankingPeriod period;

    // ==================== SCOPE GEOGRAFICO ====================

    /**
     * Codice paese (es: "IT")
     */
    @Column(length = 2)
    private String country;

    /**
     * Regione (es: "Lazio")
     */
    @Column(length = 100)
    private String region;

    /**
     * Città (es: "Roma")
     */
    @Column(length = 100)
    private String city;

    /**
     * Zona/Quartiere (es: "Centro", "Trastevere")
     */
    @Column(length = 100)
    private String zone;

    // ==================== FILTRI ====================

    /**
     * Tipo di cucina (null = tutte le cucine)
     */
    @Column(name = "cuisine_type", length = 50)
    private String cuisineType;

    /**
     * Categoria piatto specifica (es: "Pizza", "Pasta")
     */
    @Column(name = "dish_category", length = 100)
    private String dishCategory;

    // ==================== PERIODO ====================

    /**
     * Data inizio periodo
     */
    @Column(name = "period_start")
    private LocalDate periodStart;

    /**
     * Data fine periodo
     */
    @Column(name = "period_end")
    private LocalDate periodEnd;

    // ==================== STATO ====================

    /**
     * Classifica attiva
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Ultimo calcolo della classifica
     */
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    /**
     * Data creazione
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== RELAZIONI ====================

    /**
     * Entries della classifica (posizioni dei ristoranti)
     */
    @OneToMany(mappedBy = "ranking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<RankingEntry> entries = new ArrayList<>();

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se la classifica è scaduta
     */
    public boolean isExpired() {
        if (periodEnd == null) {
            return false;
        }
        return LocalDate.now().isAfter(periodEnd);
    }

    /**
     * Verifica se la classifica è nel periodo attivo
     */
    public boolean isInActivePeriod() {
        LocalDate today = LocalDate.now();
        if (periodStart != null && today.isBefore(periodStart)) {
            return false;
        }
        if (periodEnd != null && today.isAfter(periodEnd)) {
            return false;
        }
        return true;
    }

    /**
     * Restituisce il nome completo dello scope geografico
     */
    public String getFullScopeName() {
        StringBuilder sb = new StringBuilder();
        if (zone != null) {
            sb.append(zone).append(", ");
        }
        if (city != null) {
            sb.append(city).append(", ");
        }
        if (region != null) {
            sb.append(region).append(", ");
        }
        if (country != null) {
            sb.append(country);
        }
        return sb.toString().replaceAll(", $", "");
    }
}
