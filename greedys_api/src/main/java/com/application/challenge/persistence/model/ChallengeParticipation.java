package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.ParticipationStatus;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ChallengeParticipation - Partecipazione di un ristorante a una Challenge o Tournament.
 * <p>
 * Rappresenta l'iscrizione e lo stato di un ristorante in una competizione.
 * Tiene traccia di qualificazione, punteggio e progressione.
 */
@Entity
@Table(name = "challenge_participation", indexes = {
    @Index(name = "idx_participation_challenge", columnList = "challenge_id"),
    @Index(name = "idx_participation_tournament", columnList = "tournament_id"),
    @Index(name = "idx_participation_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_participation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Challenge di riferimento (opzionale se è un torneo)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    /**
     * Torneo di riferimento (opzionale se è una challenge)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    /**
     * Ristorante partecipante
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    // ==================== STATO ====================

    /**
     * Stato della partecipazione
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.REGISTERED;

    /**
     * Data di iscrizione
     */
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    /**
     * Data di qualificazione
     */
    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;

    /**
     * Data di eliminazione
     */
    @Column(name = "eliminated_at")
    private LocalDateTime eliminatedAt;

    /**
     * Motivo eliminazione/ritiro/squalifica
     */
    @Column(name = "elimination_reason", length = 500)
    private String eliminationReason;

    // ==================== QUALIFICAZIONE ====================

    /**
     * Punteggio nella fase di qualificazione
     */
    @Column(name = "qualification_score", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal qualificationScore = BigDecimal.ZERO;

    /**
     * Voti ricevuti nella fase di qualificazione
     */
    @Column(name = "qualification_votes")
    @Builder.Default
    private Integer qualificationVotes = 0;

    /**
     * Posizione nella graduatoria di qualificazione
     */
    @Column(name = "qualification_rank")
    private Integer qualificationRank;

    // ==================== FASE GIRONI ====================

    /**
     * Numero del girone assegnato (null se non ancora assegnato)
     */
    @Column(name = "group_number")
    private Integer groupNumber;

    /**
     * Partite giocate nel girone
     */
    @Column(name = "group_matches_played")
    @Builder.Default
    private Integer groupMatchesPlayed = 0;

    /**
     * Partite vinte nel girone
     */
    @Column(name = "group_wins")
    @Builder.Default
    private Integer groupWins = 0;

    /**
     * Partite perse nel girone
     */
    @Column(name = "group_losses")
    @Builder.Default
    private Integer groupLosses = 0;

    /**
     * Pareggi nel girone
     */
    @Column(name = "group_draws")
    @Builder.Default
    private Integer groupDraws = 0;

    /**
     * Punti nel girone (vittoria=3, pareggio=1, sconfitta=0)
     */
    @Column(name = "group_points")
    @Builder.Default
    private Integer groupPoints = 0;

    /**
     * Posizione nel girone
     */
    @Column(name = "group_position")
    private Integer groupPosition;

    // ==================== FASE ELIMINAZIONE ====================

    /**
     * Fase massima raggiunta
     */
    @Column(name = "furthest_phase", length = 30)
    private String furthestPhase;

    /**
     * Match eliminatorio (se eliminato)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elimination_match_id")
    private TournamentMatch eliminationMatch;

    // ==================== STATISTICHE COMPLESSIVE ====================

    /**
     * Totale voti ricevuti
     */
    @Column(name = "total_votes")
    @Builder.Default
    private Integer totalVotes = 0;

    /**
     * Punteggio totale
     */
    @Column(name = "total_score", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalScore = BigDecimal.ZERO;

    /**
     * Posizione finale
     */
    @Column(name = "final_position")
    private Integer finalPosition;

    /**
     * Premio vinto (descrizione)
     */
    @Column(name = "prize_won", length = 200)
    private String prizeWon;

    // ==================== CONTENUTI ====================

    /**
     * Numero di storie pubblicate
     */
    @Column(name = "stories_count")
    @Builder.Default
    private Integer storiesCount = 0;

    /**
     * Numero di reel pubblicati
     */
    @Column(name = "reels_count")
    @Builder.Default
    private Integer reelsCount = 0;

    // ==================== TIMESTAMP ====================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se la partecipazione è attiva
     */
    public boolean isActive() {
        return status == ParticipationStatus.REGISTERED ||
               status == ParticipationStatus.QUALIFIED ||
               status == ParticipationStatus.ACTIVE;
    }

    /**
     * Verifica se è stata eliminata
     */
    public boolean isEliminated() {
        return status == ParticipationStatus.ELIMINATED ||
               status == ParticipationStatus.DISQUALIFIED ||
               status == ParticipationStatus.WITHDRAWN;
    }

    /**
     * Verifica se ha vinto
     */
    public boolean isWinner() {
        return status == ParticipationStatus.WINNER;
    }

    /**
     * Verifica se può votare/partecipare
     */
    public boolean canParticipate() {
        return status == ParticipationStatus.QUALIFIED ||
               status == ParticipationStatus.ACTIVE;
    }

    /**
     * Calcola punti girone in base ai risultati
     */
    public int calculateGroupPoints() {
        return (groupWins != null ? groupWins * 3 : 0) +
               (groupDraws != null ? groupDraws : 0);
    }

    /**
     * Differenza punti nel girone
     */
    public int getGoalDifference() {
        // In questo caso non c'è "goal", ma potremmo usare la differenza voti
        return 0;
    }

    /**
     * Registra risultato match di girone
     */
    public void recordGroupMatchResult(boolean won, boolean draw) {
        if (groupMatchesPlayed == null) groupMatchesPlayed = 0;
        groupMatchesPlayed++;
        
        if (won) {
            if (groupWins == null) groupWins = 0;
            groupWins++;
            if (groupPoints == null) groupPoints = 0;
            groupPoints += 3;
        } else if (draw) {
            if (groupDraws == null) groupDraws = 0;
            groupDraws++;
            if (groupPoints == null) groupPoints = 0;
            groupPoints += 1;
        } else {
            if (groupLosses == null) groupLosses = 0;
            groupLosses++;
        }
    }

    /**
     * Aggiorna la fase massima raggiunta
     */
    public void updateFurthestPhase(String phase) {
        this.furthestPhase = phase;
    }

    /**
     * Nome competizione
     */
    public String getCompetitionName() {
        if (tournament != null) {
            return tournament.getName();
        }
        if (challenge != null) {
            return challenge.getName();
        }
        return "N/A";
    }
}
