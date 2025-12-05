package com.application.challenge.persistence.model;

import com.application.challenge.persistence.model.enums.MatchStatus;
import com.application.challenge.persistence.model.enums.TournamentPhase;
import com.application.restaurant.persistence.model.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TournamentMatch - Scontro diretto tra due ristoranti in un torneo.
 * <p>
 * Può essere un match di girone (round-robin) o eliminazione diretta.
 */
@Entity
@Table(name = "tournament_match", indexes = {
    @Index(name = "idx_match_tournament", columnList = "tournament_id, phase"),
    @Index(name = "idx_match_voting", columnList = "status, voting_ends_at"),
    @Index(name = "idx_match_restaurants", columnList = "restaurant1_id, restaurant2_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Torneo di appartenenza
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    // ==================== FASE E POSIZIONE ====================

    /**
     * Fase del torneo (GROUP_STAGE, QUARTER_FINALS, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentPhase phase;

    /**
     * Numero del girone (solo per GROUP_STAGE)
     */
    @Column(name = "group_number")
    private Integer groupNumber;

    /**
     * Numero del match nel bracket
     */
    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;

    /**
     * Round nel girone (1, 2, 3... per round-robin)
     */
    @Column(name = "round_number")
    private Integer roundNumber;

    // ==================== PARTECIPANTI ====================

    /**
     * Primo ristorante
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant1_id", nullable = false)
    private Restaurant restaurant1;

    /**
     * Secondo ristorante
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant2_id", nullable = false)
    private Restaurant restaurant2;

    // ==================== STATO ====================

    /**
     * Stato del match
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    /**
     * Inizio votazione
     */
    @Column(name = "voting_starts_at")
    private LocalDateTime votingStartsAt;

    /**
     * Fine votazione
     */
    @Column(name = "voting_ends_at")
    private LocalDateTime votingEndsAt;

    // ==================== RISULTATI ====================

    /**
     * Voti per restaurant1
     */
    @Builder.Default
    private Integer votes1 = 0;

    /**
     * Voti per restaurant2
     */
    @Builder.Default
    private Integer votes2 = 0;

    /**
     * Voti pesati per restaurant1 (considerando LOCAL/TOURIST)
     */
    @Column(name = "weighted_votes1", precision = 10, scale = 2)
    private java.math.BigDecimal weightedVotes1;

    /**
     * Voti pesati per restaurant2
     */
    @Column(name = "weighted_votes2", precision = 10, scale = 2)
    private java.math.BigDecimal weightedVotes2;

    /**
     * Vincitore del match
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Restaurant winner;

    /**
     * Pareggio (possibile solo in fase gironi)
     */
    @Column(name = "is_draw")
    @Builder.Default
    private Boolean isDraw = false;

    // ==================== TIMESTAMP ====================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ==================== RELAZIONI ====================

    /**
     * Voti del match
     */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MatchVote> votes = new ArrayList<>();

    // ==================== LIFECYCLE ====================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ==================== UTILITY ====================

    /**
     * Verifica se la votazione è aperta
     */
    public boolean isVotingOpen() {
        if (status != MatchStatus.VOTING) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (votingStartsAt == null || !now.isBefore(votingStartsAt)) &&
               (votingEndsAt == null || !now.isAfter(votingEndsAt));
    }

    /**
     * Verifica se la votazione è scaduta
     */
    public boolean isVotingExpired() {
        return votingEndsAt != null && LocalDateTime.now().isAfter(votingEndsAt);
    }

    /**
     * Restituisce il totale dei voti
     */
    public int getTotalVotes() {
        return (votes1 != null ? votes1 : 0) + (votes2 != null ? votes2 : 0);
    }

    /**
     * Calcola la percentuale di voti per restaurant1
     */
    public double getVotes1Percentage() {
        int total = getTotalVotes();
        if (total == 0) return 0;
        return (votes1 * 100.0) / total;
    }

    /**
     * Calcola la percentuale di voti per restaurant2
     */
    public double getVotes2Percentage() {
        int total = getTotalVotes();
        if (total == 0) return 0;
        return (votes2 * 100.0) / total;
    }

    /**
     * Determina il vincitore in base ai voti
     */
    public Restaurant determineWinner() {
        if (votes1 > votes2) {
            return restaurant1;
        } else if (votes2 > votes1) {
            return restaurant2;
        }
        return null; // pareggio
    }

    /**
     * Verifica se è una fase knockout
     */
    public boolean isKnockoutMatch() {
        return phase != null && phase.isKnockoutPhase();
    }

    /**
     * Descrizione del match
     */
    public String getMatchDescription() {
        String r1Name = restaurant1 != null ? restaurant1.getName() : "TBD";
        String r2Name = restaurant2 != null ? restaurant2.getName() : "TBD";
        return r1Name + " vs " + r2Name;
    }
}
