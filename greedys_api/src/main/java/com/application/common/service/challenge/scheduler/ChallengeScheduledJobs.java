package com.application.common.service.challenge.scheduler;

import com.application.challenge.persistence.dao.RankingRepository;
import com.application.challenge.persistence.dao.TournamentMatchRepository;
import com.application.challenge.persistence.dao.ChallengeRepository;
import com.application.challenge.persistence.model.Challenge;
import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.TournamentMatch;
import com.application.challenge.persistence.model.enums.ChallengeStatus;
import com.application.challenge.persistence.model.enums.MatchStatus;
import com.application.challenge.persistence.model.enums.RankingPeriod;
import com.application.common.service.challenge.RankingService;
import com.application.common.service.challenge.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ChallengeScheduledJobs - Job schedulati per gestione automatica challenge.
 * <p>
 * Funzionalità:
 * - Creazione automatica ranking mensili/annuali
 * - Chiusura automatica match scaduti
 * - Avanzamento automatico challenge
 * - Pulizia dati obsoleti
 * 
 * @author Greedy's System
 * @since 2025-12-03
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChallengeScheduledJobs {

    private final RankingRepository rankingRepository;
    private final TournamentMatchRepository matchRepository;
    private final ChallengeRepository challengeRepository;
    private final RankingService rankingService;
    private final TournamentService tournamentService;

    // ==================== RANKING JOBS ====================

    /**
     * Crea ranking mensili il primo giorno di ogni mese alle 00:05
     */
    @Scheduled(cron = "0 5 0 1 * *")  // 00:05 del 1° di ogni mese
    @Transactional
    public void createMonthlyRankings() {
        try {
            log.info("📊 Starting monthly rankings creation job...");

            LocalDate today = LocalDate.now();
            String monthYear = today.getMonth().name() + " " + today.getYear();

            // Trova ranking mensili attivi da rinnovare
            List<Ranking> activeMonthlyRankings = rankingRepository
                    .findByPeriodAndIsActiveTrue(RankingPeriod.MONTHLY);

            int created = 0;
            for (Ranking template : activeMonthlyRankings) {
                try {
                    // Chiudi il ranking del mese precedente
                    template.setIsActive(false);
                    template.setPeriodEnd(LocalDate.now());
                    rankingRepository.save(template);

                    // Crea nuovo ranking per il mese corrente
                    rankingService.createRanking(
                            template.getName() + " - " + monthYear,
                            template.getDescription(),
                            template.getScope(),
                            RankingPeriod.MONTHLY,
                            template.getCity(),
                            template.getRegion(),
                            template.getZone()
                    );
                    created++;
                } catch (Exception e) {
                    log.error("❌ Error creating monthly ranking for {}: {}", 
                            template.getName(), e.getMessage());
                }
            }

            log.info("✅ Monthly rankings job completed. Created {} new rankings", created);

        } catch (Exception e) {
            log.error("❌ Error in monthly rankings job: {}", e.getMessage(), e);
        }
    }

    /**
     * Crea ranking annuali il 1° gennaio alle 00:10
     */
    @Scheduled(cron = "0 10 0 1 1 *")  // 00:10 del 1° gennaio
    @Transactional
    public void createYearlyRankings() {
        try {
            log.info("📊 Starting yearly rankings creation job...");

            int year = LocalDate.now().getYear();

            // Trova ranking annuali da rinnovare
            List<Ranking> activeYearlyRankings = rankingRepository
                    .findByPeriodAndIsActiveTrue(RankingPeriod.YEARLY);

            int created = 0;
            for (Ranking template : activeYearlyRankings) {
                try {
                    // Chiudi il ranking dell'anno precedente
                    template.setIsActive(false);
                    template.setPeriodEnd(LocalDate.now());
                    rankingRepository.save(template);

                    // Crea nuovo ranking per l'anno corrente
                    rankingService.createRanking(
                            template.getName() + " " + year,
                            template.getDescription(),
                            template.getScope(),
                            RankingPeriod.YEARLY,
                            template.getCity(),
                            template.getRegion(),
                            template.getZone()
                    );
                    created++;
                } catch (Exception e) {
                    log.error("❌ Error creating yearly ranking for {}: {}", 
                            template.getName(), e.getMessage());
                }
            }

            log.info("✅ Yearly rankings job completed. Created {} new rankings", created);

        } catch (Exception e) {
            log.error("❌ Error in yearly rankings job: {}", e.getMessage(), e);
        }
    }

    // ==================== MATCH JOBS ====================

    /**
     * Chiude automaticamente i match con votazione scaduta - ogni 15 minuti
     */
    @Scheduled(cron = "0 */15 * * * *")  // Ogni 15 minuti
    @Transactional
    public void closeExpiredMatchVoting() {
        try {
            log.debug("🗳️ Checking for expired match voting...");

            List<TournamentMatch> expiredMatches = matchRepository
                    .findByStatusAndVotingEndsAtBefore(MatchStatus.VOTING, LocalDateTime.now());

            int closed = 0;
            for (TournamentMatch match : expiredMatches) {
                try {
                    tournamentService.closeMatchVoting(match.getId(), null);
                    closed++;
                    log.info("🔒 Auto-closed voting for match {} ({})", 
                            match.getId(), match.getMatchDescription());
                } catch (Exception e) {
                    log.error("❌ Error closing match {}: {}", match.getId(), e.getMessage());
                }
            }

            if (closed > 0) {
                log.info("✅ Expired match voting job completed. Closed {} matches", closed);
            }

        } catch (Exception e) {
            log.error("❌ Error in expired match voting job: {}", e.getMessage(), e);
        }
    }

    // ==================== CHALLENGE JOBS ====================

    /**
     * Avvia automaticamente le challenge alla data di inizio - ogni ora
     */
    @Scheduled(cron = "0 0 * * * *")  // Ogni ora
    @Transactional
    public void startScheduledChallenges() {
        try {
            log.debug("🏁 Checking for challenges to start...");

            LocalDateTime now = LocalDateTime.now();
            List<Challenge> challengesToStart = challengeRepository
                    .findByStatusAndStartDateBefore(ChallengeStatus.REGISTRATION, now);

            int started = 0;
            for (Challenge challenge : challengesToStart) {
                try {
                    challenge.setStatus(ChallengeStatus.ACTIVE);
                    challengeRepository.save(challenge);
                    started++;
                    log.info("🚀 Auto-started challenge: {} ({})", 
                            challenge.getName(), challenge.getId());
                } catch (Exception e) {
                    log.error("❌ Error starting challenge {}: {}", 
                            challenge.getId(), e.getMessage());
                }
            }

            if (started > 0) {
                log.info("✅ Challenge start job completed. Started {} challenges", started);
            }

        } catch (Exception e) {
            log.error("❌ Error in challenge start job: {}", e.getMessage(), e);
        }
    }

    /**
     * Termina automaticamente le challenge alla data di fine - ogni ora
     */
    @Scheduled(cron = "0 5 * * * *")  // 5 minuti dopo l'ora
    @Transactional
    public void endExpiredChallenges() {
        try {
            log.debug("🏁 Checking for challenges to end...");

            LocalDateTime now = LocalDateTime.now();
            List<Challenge> challengesToEnd = challengeRepository
                    .findByStatusInAndEndDateBefore(
                            List.of(ChallengeStatus.ACTIVE, ChallengeStatus.VOTING), 
                            now);

            int ended = 0;
            for (Challenge challenge : challengesToEnd) {
                try {
                    challenge.setStatus(ChallengeStatus.COMPLETED);
                    challenge.setCompletedAt(LocalDateTime.now());
                    challengeRepository.save(challenge);
                    ended++;
                    log.info("🏆 Auto-completed challenge: {} ({})", 
                            challenge.getName(), challenge.getId());
                } catch (Exception e) {
                    log.error("❌ Error ending challenge {}: {}", 
                            challenge.getId(), e.getMessage());
                }
            }

            if (ended > 0) {
                log.info("✅ Challenge end job completed. Ended {} challenges", ended);
            }

        } catch (Exception e) {
            log.error("❌ Error in challenge end job: {}", e.getMessage(), e);
        }
    }

    // ==================== CLEANUP JOBS ====================

    /**
     * Pulizia dati obsoleti - ogni domenica alle 3:00
     */
    @Scheduled(cron = "0 0 3 * * SUN")  // Domenica alle 3:00
    @Transactional
    public void cleanupOldData() {
        try {
            log.info("🧹 Starting challenge data cleanup job...");

            LocalDateTime cutoff = LocalDateTime.now().minusMonths(12);

            // Conta challenge vecchie completate (per logging)
            long oldChallenges = challengeRepository.countByStatusAndCompletedAtBefore(
                    ChallengeStatus.COMPLETED, cutoff);

            // Non eliminiamo, solo log per ora
            // In futuro si può implementare archiviazione

            log.info("📊 Cleanup stats: {} completed challenges older than 12 months", 
                    oldChallenges);

            log.info("✅ Challenge cleanup job completed");

        } catch (Exception e) {
            log.error("❌ Error in challenge cleanup job: {}", e.getMessage(), e);
        }
    }

    // ==================== STATS JOBS ====================

    /**
     * Aggiorna statistiche ranking - ogni giorno alle 4:00
     */
    @Scheduled(cron = "0 0 4 * * *")  // Ogni giorno alle 4:00
    @Transactional
    public void updateRankingStatistics() {
        try {
            log.info("📈 Starting ranking statistics update job...");

            List<Ranking> activeRankings = rankingRepository.findByIsActiveTrue();

            int updated = 0;
            for (Ranking ranking : activeRankings) {
                try {
                    // Ricalcola posizioni
                    rankingService.recalculateRanking(ranking.getId());
                    updated++;
                } catch (Exception e) {
                    log.error("❌ Error updating ranking {}: {}", 
                            ranking.getId(), e.getMessage());
                }
            }

            log.info("✅ Ranking statistics job completed. Updated {} rankings", updated);

        } catch (Exception e) {
            log.error("❌ Error in ranking statistics job: {}", e.getMessage(), e);
        }
    }
}
