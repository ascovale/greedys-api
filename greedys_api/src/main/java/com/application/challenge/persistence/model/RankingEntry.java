package com.application.challenge.persistence.model;

import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RankingEntry - Posizione di un ristorante in una classifica.
 * <p>
 * Rappresenta l'entry di un singolo ristorante in un Ranking,
 * con posizione, score e statistiche sui voti.
 */
@Entity
@Table(name = "ranking_entry", indexes = {
    @Index(name = "idx_entry_position", columnList = "ranking_id, position"),
    @Index(name = "idx_entry_restaurant", columnList = "restaurant_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ranking_restaurant", columnNames = {"ranking_id", "restaurant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ranking di appartenenza
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ranking_id", nullable = false)
    private Ranking ranking;

    /**
     * Ristorante in classifica
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    // ==================== POSIZIONE ====================

    /**
     * Posizione attuale (1 = primo)
     */
    @Column(nullable = false)
    private Integer position;

    /**
     * Posizione precedente (per mostrare trend ↑↓)
     */
    @Column(name = "previous_position")
    private Integer previousPosition;

    // ==================== SCORE ====================

    /**
     * Punteggio calcolato (formula complessa)
     */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal score;

    /**
     * Media voti (1-5 stelle)
     */
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    // ==================== STATISTICHE VOTI ====================

    /**
     * Totale voti ricevuti
     */
    @Column(name = "total_votes")
    @Builder.Default
    private Integer totalVotes = 0;

    /**
     * Voti da clienti LOCAL
     */
    @Column(name = "local_votes")
    @Builder.Default
    private Integer localVotes = 0;

    /**
     * Voti da clienti TOURIST
     */
    @Column(name = "tourist_votes")
    @Builder.Default
    private Integer touristVotes = 0;

    /**
     * Voti da clienti VERIFIED
     */
    @Column(name = "verified_votes")
    @Builder.Default
    private Integer verifiedVotes = 0;

    // ==================== STATISTICHE PRENOTAZIONI ====================

    /**
     * Prenotazioni SEATED nel periodo della classifica
     */
    @Column(name = "seated_reservations")
    @Builder.Default
    private Integer seatedReservations = 0;

    // ==================== TIMESTAMP ====================

    /**
     * Quando è stato calcolato questo entry
     */
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    // ==================== UTILITY ====================

    /**
     * Calcola il trend rispetto alla posizione precedente
     * @return positivo = salito, negativo = sceso, 0 = stabile
     */
    public int getTrend() {
        if (previousPosition == null) {
            return 0; // nuovo in classifica
        }
        return previousPosition - position; // es: era 5, ora è 3 → trend = +2
    }

    /**
     * Restituisce l'emoji del trend
     */
    public String getTrendEmoji() {
        int trend = getTrend();
        if (trend > 0) {
            return "↑";
        } else if (trend < 0) {
            return "↓";
        }
        return "→";
    }

    /**
     * Verifica se è nuovo in classifica
     */
    public boolean isNewEntry() {
        return previousPosition == null;
    }

    /**
     * Verifica se è sul podio (top 3)
     */
    public boolean isOnPodium() {
        return position != null && position <= 3;
    }

    /**
     * Verifica se è in top 10
     */
    public boolean isInTopTen() {
        return position != null && position <= 10;
    }

    /**
     * Calcola percentuale voti LOCAL
     */
    public BigDecimal getLocalVotePercentage() {
        if (totalVotes == null || totalVotes == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(localVotes)
            .divide(BigDecimal.valueOf(totalVotes), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
}
