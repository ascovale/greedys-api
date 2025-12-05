package com.application.common.service.challenge;

import com.application.challenge.persistence.dao.MatchVoteRepository;
import com.application.challenge.persistence.dao.RankingVoteRepository;
import com.application.challenge.persistence.dao.TournamentMatchRepository;
import com.application.challenge.persistence.dao.RankingRepository;
import com.application.challenge.persistence.dao.RankingEntryRepository;
import com.application.challenge.persistence.model.MatchVote;
import com.application.challenge.persistence.model.RankingVote;
import com.application.challenge.persistence.model.TournamentMatch;
import com.application.challenge.persistence.model.Ranking;
import com.application.challenge.persistence.model.RankingEntry;
import com.application.challenge.persistence.model.enums.MatchStatus;
import com.application.challenge.persistence.model.enums.VoterType;
import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.model.audit.ChallengeAuditLog;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.common.service.audit.AuditService;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VoteService - Gestione voti per match di torneo e ranking.
 * <p>
 * Funzionalità principali:
 * - Voto nei match di torneo
 * - Voto nei ranking
 * - Verifica voti tramite prenotazioni
 * - Calcolo pesi in base a voterType
 * - Antifrode base
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoteService {

    private final MatchVoteRepository matchVoteRepository;
    private final RankingVoteRepository rankingVoteRepository;
    private final TournamentMatchRepository matchRepository;
    private final RankingRepository rankingRepository;
    private final RankingEntryRepository rankingEntryRepository;
    private final CustomerDAO customerDAO;
    private final RestaurantDAO restaurantDAO;
    private final ReservationDAO reservationDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // ==================== MATCH VOTING ====================

    /**
     * Vota in un match di torneo
     */
    public MatchVote voteInMatch(Long matchId, Long customerId, Long restaurantId,
                                  VoterType voterType, String ipAddress, String userAgent) {
        log.info("Customer {} voting for restaurant {} in match {}", customerId, restaurantId, matchId);

        // Recupera match
        TournamentMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));

        // Verifica votazione aperta
        if (match.getStatus() != MatchStatus.VOTING) {
            throw new IllegalStateException("Voting is not open for this match");
        }

        if (!match.isVotingOpen()) {
            throw new IllegalStateException("Voting period has ended for this match");
        }

        // Verifica ristorante valido
        boolean isValidRestaurant = 
            (match.getRestaurant1() != null && match.getRestaurant1().getId().equals(restaurantId)) ||
            (match.getRestaurant2() != null && match.getRestaurant2().getId().equals(restaurantId));

        if (!isValidRestaurant) {
            throw new IllegalArgumentException("Restaurant is not participating in this match");
        }

        // Verifica voto non già espresso
        if (matchVoteRepository.existsByMatchIdAndCustomerId(matchId, customerId)) {
            throw new IllegalStateException("Customer has already voted in this match");
        }

        // Recupera customer e restaurant
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));

        // Calcola peso voto
        BigDecimal voteWeight = calculateVoteWeight(voterType, false);

        // Crea voto
        MatchVote vote = MatchVote.builder()
                .match(match)
                .customer(customer)
                .votedRestaurant(restaurant)
                .voterType(voterType)
                .voteWeight(voteWeight)
                .isVerified(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .votedAt(LocalDateTime.now())
                .build();

        MatchVote saved = matchVoteRepository.save(vote);

        // Aggiorna conteggi nel match
        updateMatchVoteCounts(match, restaurantId);

        // Audit
        auditService.auditMatchVoteCast(
                saved.getId(),
                matchId,
                match.getTournament().getId(),
                restaurantId,
                customerId,
                "voterType=" + voterType + ", weight=" + voteWeight,
                ipAddress
        );

        // Evento
        publishEvent(
                EventType.MATCH_VOTE_CAST,
                "MATCH_VOTE",
                saved.getId(),
                buildMatchVotePayload(saved)
        );

        log.info("Vote {} recorded in match {} for restaurant {}", saved.getId(), matchId, restaurantId);
        return saved;
    }

    /**
     * Aggiorna i conteggi dei voti nel match
     */
    private void updateMatchVoteCounts(TournamentMatch match, Long votedRestaurantId) {
        // Conta voti per restaurant1
        long votes1 = matchVoteRepository.countVotesForRestaurantInMatch(
                match.getId(), match.getRestaurant1().getId());
        // Conta voti per restaurant2
        long votes2 = matchVoteRepository.countVotesForRestaurantInMatch(
                match.getId(), match.getRestaurant2().getId());

        match.setVotes1((int) votes1);
        match.setVotes2((int) votes2);

        // Aggiorna anche voti pesati
        BigDecimal weighted1 = matchVoteRepository.getWeightedVotesForRestaurant(
                match.getId(), match.getRestaurant1().getId());
        BigDecimal weighted2 = matchVoteRepository.getWeightedVotesForRestaurant(
                match.getId(), match.getRestaurant2().getId());

        match.setWeightedVotes1(weighted1);
        match.setWeightedVotes2(weighted2);

        matchRepository.save(match);
    }

    /**
     * Verifica se un customer può votare in un match
     */
    @Transactional(readOnly = true)
    public boolean canVoteInMatch(Long matchId, Long customerId) {
        // Verifica match in votazione
        TournamentMatch match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getStatus() != MatchStatus.VOTING || !match.isVotingOpen()) {
            return false;
        }

        // Verifica non già votato
        return !matchVoteRepository.existsByMatchIdAndCustomerId(matchId, customerId);
    }

    /**
     * Recupera voto di un customer in un match
     */
    @Transactional(readOnly = true)
    public MatchVote getCustomerVoteInMatch(Long matchId, Long customerId) {
        return matchVoteRepository.findByMatchIdAndCustomerId(matchId, customerId).orElse(null);
    }

    /**
     * Lista voti di un match
     */
    @Transactional(readOnly = true)
    public List<MatchVote> getMatchVotes(Long matchId) {
        return matchVoteRepository.findByMatchId(matchId);
    }

    // ==================== RANKING VOTING ====================

    /**
     * Vota in un ranking
     */
    public RankingVote voteInRanking(Long rankingId, Long restaurantId, Long customerId,
                                      Long reservationId, Integer rating, VoterType voterType, 
                                      String ipAddress, String comment) {
        log.info("Customer {} voting for restaurant {} in ranking {} with rating {}",
                customerId, restaurantId, rankingId, rating);

        // Verifica ranking
        Ranking ranking = rankingRepository.findById(rankingId)
                .orElseThrow(() -> new EntityNotFoundException("Ranking not found: " + rankingId));

        // Verifica ristorante nel ranking
        boolean restaurantInRanking = rankingEntryRepository
                .existsByRankingIdAndRestaurantId(rankingId, restaurantId);
        if (!restaurantInRanking) {
            throw new IllegalArgumentException("Restaurant is not in this ranking");
        }

        // Verifica voto non già espresso
        if (rankingVoteRepository.existsByRankingIdAndRestaurantIdAndVoterId(
                rankingId, restaurantId, customerId)) {
            throw new IllegalStateException("Customer has already voted for this restaurant in this ranking");
        }

        // Verifica rating valido
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Recupera entità
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));
        Reservation reservation = reservationDAO.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + reservationId));

        // Crea voto
        RankingVote vote = RankingVote.builder()
                .voter(customer)
                .restaurant(restaurant)
                .reservation(reservation)
                .ranking(ranking)
                .voterType(voterType)
                .rating(rating)
                .score(java.math.BigDecimal.valueOf(rating)) // Score iniziale = rating
                .comment(comment)
                .ipAddress(ipAddress)
                .isVerified(false)
                .isSuspicious(false)
                .votedAt(LocalDateTime.now())
                .build();

        RankingVote saved = rankingVoteRepository.save(vote);

        // Aggiorna entry nel ranking
        updateRankingEntryScore(rankingId, restaurantId);

        // Audit
        auditService.auditRankingVoteCast(
                saved.getId(),
                rankingId,
                restaurantId,
                customerId,
                "rating=" + rating + ", voterType=" + voterType,
                ipAddress
        );

        // Evento
        publishEvent(
                EventType.RANKING_VOTE_CAST,
                "RANKING_VOTE",
                saved.getId(),
                buildRankingVotePayload(saved, rankingId)
        );

        log.info("Vote {} recorded in ranking {} for restaurant {}", saved.getId(), rankingId, restaurantId);
        return saved;
    }

    /**
     * Aggiorna il punteggio di un'entry nel ranking
     */
    private void updateRankingEntryScore(Long rankingId, Long restaurantId) {
        RankingEntry entry = rankingEntryRepository
                .findByRankingIdAndRestaurantId(rankingId, restaurantId)
                .orElse(null);

        if (entry != null) {
            // Calcola nuovo punteggio medio
            Double avgScore = rankingVoteRepository.getAverageScoreByRankingAndRestaurant(rankingId, restaurantId);
            long voteCount = rankingVoteRepository.countByRankingAndRestaurant(rankingId, restaurantId);

            if (avgScore != null) {
                entry.setScore(BigDecimal.valueOf(avgScore));
            }
            entry.setTotalVotes((int) voteCount);

            rankingEntryRepository.save(entry);
        }
    }

    /**
     * Verifica se un customer può votare per un ristorante in un ranking
     */
    @Transactional(readOnly = true)
    public boolean canVoteInRanking(Long rankingId, Long restaurantId, Long customerId) {
        return !rankingVoteRepository.existsByRankingIdAndRestaurantIdAndVoterId(
                rankingId, restaurantId, customerId);
    }

    /**
     * Recupera voto di un customer per un ristorante in un ranking
     */
    @Transactional(readOnly = true)
    public RankingVote getCustomerVoteInRanking(Long rankingId, Long restaurantId, Long customerId) {
        return rankingVoteRepository.findByRankingIdAndRestaurantIdAndVoterId(
                rankingId, restaurantId, customerId).orElse(null);
    }

    /**
     * Lista voti di un ranking per un ristorante
     */
    @Transactional(readOnly = true)
    public List<RankingVote> getRankingVotesForRestaurant(Long rankingId, Long restaurantId) {
        return rankingVoteRepository.findByRestaurantId(restaurantId);
    }

    // ==================== VOTE WEIGHT CALCULATION ====================

    /**
     * Calcola il peso del voto in base al tipo di votante
     */
    private BigDecimal calculateVoteWeight(VoterType voterType, boolean isVerified) {
        BigDecimal baseWeight = voterType != null ? voterType.getVoteWeight() : BigDecimal.ONE;

        if (isVerified) {
            // Voto verificato: bonus 50%
            return baseWeight.multiply(new BigDecimal("1.5"));
        }

        return baseWeight;
    }

    // ==================== ANTI-FRAUD ====================

    /**
     * Controlla se ci sono segnali di frode
     */
    @Transactional(readOnly = true)
    public boolean checkForMatchVoteFraud(Long matchId, Long customerId, String ipAddress, String deviceId) {
        // Controlla voti multipli dallo stesso IP
        if (ipAddress != null) {
            long ipVotes = matchVoteRepository.countByIpAndMatch(ipAddress, matchId);
            if (ipVotes >= 3) {
                log.warn("Suspicious: {} votes from same IP {} in match {}", ipVotes, ipAddress, matchId);
                return true;
            }
        }

        // Controlla voti multipli dallo stesso device
        if (deviceId != null) {
            long deviceVotes = matchVoteRepository.countByDeviceAndMatch(deviceId, matchId);
            if (deviceVotes >= 2) {
                log.warn("Suspicious: {} votes from same device {} in match {}", deviceVotes, deviceId, matchId);
                return true;
            }
        }

        // Controlla frequenza voti del customer
        long recentVotes = matchVoteRepository.countRecentVotesByCustomer(
                customerId, LocalDateTime.now().minusHours(1));
        if (recentVotes >= 10) {
            log.warn("Suspicious: customer {} cast {} votes in last hour", customerId, recentVotes);
            return true;
        }

        return false;
    }

    // ==================== STATISTICS ====================

    /**
     * Statistiche voti per un match
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMatchVoteStatistics(Long matchId) {
        TournamentMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));

        Map<String, Object> stats = new HashMap<>();
        stats.put("matchId", matchId);
        stats.put("totalVotes", matchVoteRepository.countByMatchId(matchId));
        stats.put("votes1", match.getVotes1());
        stats.put("votes2", match.getVotes2());
        stats.put("weightedVotes1", match.getWeightedVotes1());
        stats.put("weightedVotes2", match.getWeightedVotes2());
        stats.put("verifiedVotes", matchVoteRepository.countVerifiedVotesByMatch(matchId));

        // Breakdown per voterType
        List<Object[]> voterTypeBreakdown = matchVoteRepository.countByMatchGroupedByVoterType(matchId);
        Map<String, Long> byVoterType = new HashMap<>();
        for (Object[] row : voterTypeBreakdown) {
            byVoterType.put(((VoterType) row[0]).name(), (Long) row[1]);
        }
        stats.put("byVoterType", byVoterType);

        return stats;
    }

    /**
     * Statistiche voti per un ranking
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRankingVoteStatistics(Long rankingId, Long restaurantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("rankingId", rankingId);
        stats.put("restaurantId", restaurantId);

        long totalVotes = rankingVoteRepository.countByRankingAndRestaurant(rankingId, restaurantId);
        Double avgScore = rankingVoteRepository.getAverageScoreByRankingAndRestaurant(rankingId, restaurantId);
        long verifiedVotes = rankingVoteRepository.countVerifiedByRankingAndRestaurant(rankingId, restaurantId);

        stats.put("totalVotes", totalVotes);
        stats.put("averageScore", avgScore != null ? avgScore : 0.0);
        stats.put("verifiedVotes", verifiedVotes);

        // Breakdown per voterType
        List<Object[]> voterTypeBreakdown = rankingVoteRepository
                .countByRestaurantGroupedByVoterType(rankingId, restaurantId);
        Map<String, Long> byVoterType = new HashMap<>();
        for (Object[] row : voterTypeBreakdown) {
            byVoterType.put(((VoterType) row[0]).name(), (Long) row[1]);
        }
        stats.put("byVoterType", byVoterType);

        return stats;
    }

    // ==================== EVENT PUBLISHING ====================

    private void publishEvent(EventType eventType, String aggregateType, Long aggregateId, String payload) {
        String eventId = UUID.randomUUID().toString();

        EventOutbox event = EventOutbox.builder()
                .eventId(eventId)
                .eventType(eventType.name())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .status(EventOutbox.Status.PENDING)
                .createdAt(Instant.now())
                .build();

        eventOutboxDAO.save(event);
        log.debug("Published event {}: {}", eventType, eventId);
    }

    private String buildMatchVotePayload(MatchVote vote) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("voteId", vote.getId());
            data.put("matchId", vote.getMatch().getId());
            data.put("tournamentId", vote.getMatch().getTournament().getId());
            data.put("customerId", vote.getCustomer().getId());
            data.put("votedRestaurantId", vote.getVotedRestaurant().getId());
            data.put("votedRestaurantName", vote.getVotedRestaurant().getName());
            data.put("voterType", vote.getVoterType().name());
            data.put("voteWeight", vote.getVoteWeight());
            data.put("isVerified", vote.getIsVerified());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error building match vote payload", e);
            return "{}";
        }
    }

    private String buildRankingVotePayload(RankingVote vote, Long rankingId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("voteId", vote.getId());
            data.put("rankingId", rankingId);
            data.put("customerId", vote.getVoter().getId());
            data.put("restaurantId", vote.getRestaurant().getId());
            data.put("restaurantName", vote.getRestaurant().getName());
            data.put("rating", vote.getRating());
            data.put("voterType", vote.getVoterType().name());
            data.put("isVerified", vote.getIsVerified());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error building ranking vote payload", e);
            return "{}";
        }
    }
}
