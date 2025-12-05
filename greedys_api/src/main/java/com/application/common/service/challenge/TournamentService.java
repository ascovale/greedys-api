package com.application.common.service.challenge;

import com.application.challenge.persistence.dao.*;
import com.application.challenge.persistence.model.*;
import com.application.challenge.persistence.model.enums.*;
import com.application.common.domain.event.EventType;
import com.application.common.persistence.dao.EventOutboxDAO;
import com.application.common.persistence.model.notification.EventOutbox;
import com.application.common.persistence.model.audit.ChallengeAuditLog;
import com.application.common.service.audit.AuditService;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchRepository matchRepository;
    private final ChallengeParticipationRepository participationRepository;
    private final RestaurantDAO restaurantDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // ==================== CRUD TOURNAMENT ====================

    public Tournament createTournament(Tournament tournament, Long createdByUserId) {
        log.info("Creating tournament: {}", tournament.getName());
        
        validateTournamentDates(tournament);
        
        if (tournament.getStatus() == null) {
            tournament.setStatus(TournamentStatus.DRAFT);
        }
        
        Tournament saved = tournamentRepository.save(tournament);
        
        auditService.auditTournamentCreated(
            saved.getId(),
            createdByUserId,
            ChallengeAuditLog.UserType.ADMIN,
            saved
        );
        
        publishEvent(
            EventType.TOURNAMENT_CREATED,
            "TOURNAMENT",
            saved.getId(),
            buildTournamentPayload(saved, null)
        );
        
        log.info("Created tournament {}: {}", saved.getId(), saved.getName());
        return saved;
    }

    public Tournament updateTournament(Long tournamentId, Tournament updates, Long updatedByUserId) {
        Tournament tournament = findById(tournamentId);
        
        if (tournament.getStatus() == TournamentStatus.COMPLETED || 
            tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update tournament in status: " + tournament.getStatus());
        }
        
        String oldStatus = tournament.getStatus().name();
        
        if (updates.getName() != null) {
            tournament.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            tournament.setDescription(updates.getDescription());
        }
        if (updates.getRules() != null) {
            tournament.setRules(updates.getRules());
        }
        if (updates.getCity() != null) {
            tournament.setCity(updates.getCity());
        }
        if (updates.getZone() != null) {
            tournament.setZone(updates.getZone());
        }
        if (updates.getCuisineType() != null) {
            tournament.setCuisineType(updates.getCuisineType());
        }
        if (updates.getMaxParticipants() != null) {
            tournament.setMaxParticipants(updates.getMaxParticipants());
        }
        if (updates.getMatchVotingHours() != null) {
            tournament.setMatchVotingHours(updates.getMatchVotingHours());
        }
        
        Tournament saved = tournamentRepository.save(tournament);
        
        log.info("Updated tournament {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Tournament findById(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new EntityNotFoundException("Tournament not found: " + tournamentId));
    }

    @Transactional(readOnly = true)
    public List<Tournament> findByStatus(TournamentStatus status) {
        return tournamentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Tournament> findByCity(String city) {
        return tournamentRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Tournament> findWithOpenRegistration() {
        return tournamentRepository.findWithOpenRegistration(LocalDate.now());
    }

    // ==================== LIFECYCLE ====================

    public Tournament openRegistration(Long tournamentId, Long userId) {
        Tournament tournament = findById(tournamentId);
        
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new IllegalStateException("Can only open registration from DRAFT status");
        }
        
        String oldStatus = tournament.getStatus().name();
        tournament.setStatus(TournamentStatus.REGISTRATION);
        Tournament saved = tournamentRepository.save(tournament);
        
        auditService.auditTournamentPhaseChanged(
            saved.getId(),
            userId,
            ChallengeAuditLog.UserType.ADMIN,
            oldStatus,
            saved.getStatus().name()
        );
        
        publishEvent(
            EventType.TOURNAMENT_REGISTRATION_OPENED,
            "TOURNAMENT",
            saved.getId(),
            buildTournamentPayload(saved, null)
        );
        
        log.info("Opened registration for tournament {}", saved.getId());
        return saved;
    }

    public Tournament startTournament(Long tournamentId, Long userId) {
        Tournament tournament = findById(tournamentId);
        
        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new IllegalStateException("Can only start tournament from REGISTRATION status");
        }
        
        long participantCount = participationRepository.countByTournamentId(tournamentId);
        if (participantCount < 4) {
            throw new IllegalStateException("Need at least 4 participants to start tournament");
        }
        
        String oldStatus = tournament.getStatus().name();
        tournament.setStatus(TournamentStatus.ONGOING);
        tournament.setCurrentPhase(TournamentPhase.GROUP_STAGE);
        Tournament saved = tournamentRepository.save(tournament);
        
        createGroupStageMatches(saved);
        
        auditService.auditTournamentPhaseChanged(
            saved.getId(),
            userId,
            ChallengeAuditLog.UserType.ADMIN,
            oldStatus,
            saved.getStatus().name()
        );
        
        publishEvent(
            EventType.TOURNAMENT_STATUS_CHANGED,
            "TOURNAMENT",
            saved.getId(),
            buildTournamentPayload(saved, null)
        );
        
        log.info("Started tournament {} with {} participants", saved.getId(), participantCount);
        return saved;
    }

    public Tournament completeTournament(Long tournamentId, Long userId) {
        Tournament tournament = findById(tournamentId);
        
        if (tournament.getStatus() != TournamentStatus.ONGOING) {
            throw new IllegalStateException("Can only complete tournament from ONGOING status");
        }
        
        String oldStatus = tournament.getStatus().name();
        tournament.setStatus(TournamentStatus.COMPLETED);
        tournament.setTournamentEnd(LocalDate.now());
        Tournament saved = tournamentRepository.save(tournament);
        
        auditService.auditTournamentPhaseChanged(
            saved.getId(),
            userId,
            ChallengeAuditLog.UserType.ADMIN,
            oldStatus,
            saved.getStatus().name()
        );
        
        publishEvent(
            EventType.TOURNAMENT_COMPLETED,
            "TOURNAMENT",
            saved.getId(),
            buildTournamentPayload(saved, null)
        );
        
        log.info("Completed tournament {}", saved.getId());
        return saved;
    }

    public Tournament cancelTournament(Long tournamentId, String reason, Long userId) {
        Tournament tournament = findById(tournamentId);
        
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed tournament");
        }
        
        String oldStatus = tournament.getStatus().name();
        tournament.setStatus(TournamentStatus.CANCELLED);
        Tournament saved = tournamentRepository.save(tournament);
        
        auditService.auditTournamentPhaseChanged(
            saved.getId(),
            userId,
            ChallengeAuditLog.UserType.ADMIN,
            oldStatus,
            "CANCELLED"
        );
        
        log.info("Cancelled tournament {}: {}", saved.getId(), reason);
        return saved;
    }

    // ==================== REGISTRATION ====================

    public ChallengeParticipation registerRestaurant(Long tournamentId, Long restaurantId, Long userId) {
        Tournament tournament = findById(tournamentId);
        
        if (!tournament.isRegistrationOpen()) {
            throw new IllegalStateException("Registration is not open for this tournament");
        }
        
        if (!tournament.hasAvailableSlots()) {
            throw new IllegalStateException("Tournament is full");
        }
        
        if (participationRepository.existsByTournamentIdAndRestaurantId(tournamentId, restaurantId)) {
            throw new IllegalStateException("Restaurant is already registered for this tournament");
        }
        
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));
        
        ChallengeParticipation participation = ChallengeParticipation.builder()
            .tournament(tournament)
            .restaurant(restaurant)
            .status(ParticipationStatus.REGISTERED)
            .registeredAt(LocalDateTime.now())
            .build();
        
        ChallengeParticipation saved = participationRepository.save(participation);
        
        auditService.auditRestaurantRegistered(
            saved.getId(),
            null,
            tournamentId,
            restaurantId,
            userId,
            ChallengeAuditLog.UserType.RESTAURANT_USER
        );
        
        log.info("Restaurant {} registered for tournament {}", restaurantId, tournamentId);
        return saved;
    }

    public void withdrawRestaurant(Long tournamentId, Long restaurantId, String reason, Long userId) {
        ChallengeParticipation participation = participationRepository
            .findByTournamentIdAndRestaurantId(tournamentId, restaurantId)
            .orElseThrow(() -> new EntityNotFoundException("Participation not found"));
        
        participation.setStatus(ParticipationStatus.WITHDRAWN);
        participation.setEliminationReason(reason);
        participation.setEliminatedAt(LocalDateTime.now());
        participationRepository.save(participation);
        
        log.info("Restaurant {} withdrawn from tournament {}", restaurantId, tournamentId);
    }

    @Transactional(readOnly = true)
    public List<ChallengeParticipation> getParticipants(Long tournamentId) {
        return participationRepository.findByTournamentId(tournamentId);
    }

    // ==================== MATCH MANAGEMENT ====================

    private void createGroupStageMatches(Tournament tournament) {
        List<ChallengeParticipation> participants = participationRepository
            .findByTournamentIdAndStatus(tournament.getId(), ParticipationStatus.REGISTERED);
        
        int groupCount = tournament.getGroupCount();
        int groupSize = tournament.getGroupSize();
        
        Collections.shuffle(participants);
        
        for (int g = 0; g < groupCount && g * groupSize < participants.size(); g++) {
            int start = g * groupSize;
            int end = Math.min(start + groupSize, participants.size());
            List<ChallengeParticipation> groupParticipants = participants.subList(start, end);
            
            for (ChallengeParticipation p : groupParticipants) {
                p.setGroupNumber(g + 1);
                p.setStatus(ParticipationStatus.ACTIVE);
                participationRepository.save(p);
            }
            
            createRoundRobinMatches(tournament, g + 1, groupParticipants);
        }
        
        log.info("Created group stage matches for tournament {}", tournament.getId());
    }

    private void createRoundRobinMatches(Tournament tournament, int groupNumber, 
                                          List<ChallengeParticipation> participants) {
        int matchNumber = 0;
        int roundNumber = 1;
        
        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                TournamentMatch match = TournamentMatch.builder()
                    .tournament(tournament)
                    .phase(TournamentPhase.GROUP_STAGE)
                    .groupNumber(groupNumber)
                    .matchNumber(++matchNumber)
                    .roundNumber(roundNumber)
                    .restaurant1(participants.get(i).getRestaurant())
                    .restaurant2(participants.get(j).getRestaurant())
                    .status(MatchStatus.SCHEDULED)
                    .votes1(0)
                    .votes2(0)
                    .build();
                
                matchRepository.save(match);
            }
            
            if (matchNumber % 2 == 0) {
                roundNumber++;
            }
        }
    }

    public TournamentMatch createMatch(Long tournamentId, TournamentPhase phase, 
                                       Long restaurant1Id, Long restaurant2Id, 
                                       int matchNumber, Long userId) {
        Tournament tournament = findById(tournamentId);
        
        Restaurant restaurant1 = restaurantDAO.findById(restaurant1Id)
            .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurant1Id));
        Restaurant restaurant2 = restaurantDAO.findById(restaurant2Id)
            .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurant2Id));
        
        TournamentMatch match = TournamentMatch.builder()
            .tournament(tournament)
            .phase(phase)
            .matchNumber(matchNumber)
            .restaurant1(restaurant1)
            .restaurant2(restaurant2)
            .status(MatchStatus.SCHEDULED)
            .votes1(0)
            .votes2(0)
            .build();
        
        TournamentMatch saved = matchRepository.save(match);
        
        publishEvent(
            EventType.MATCH_SCHEDULED,
            "MATCH",
            saved.getId(),
            buildMatchPayload(saved)
        );
        
        log.info("Created match {}: {}", saved.getId(), saved.getMatchDescription());
        return saved;
    }

    public TournamentMatch startMatchVoting(Long matchId, Long userId) {
        TournamentMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
        
        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Can only start voting for SCHEDULED matches");
        }
        
        Tournament tournament = match.getTournament();
        int votingHours = tournament.getMatchVotingHours() != null ? tournament.getMatchVotingHours() : 48;
        
        match.setStatus(MatchStatus.VOTING);
        match.setVotingStartsAt(LocalDateTime.now());
        match.setVotingEndsAt(LocalDateTime.now().plusHours(votingHours));
        
        TournamentMatch saved = matchRepository.save(match);
        
        publishEvent(
            EventType.MATCH_VOTING_OPENED,
            "MATCH",
            saved.getId(),
            buildMatchPayload(saved)
        );
        
        log.info("Started voting for match {} until {}", saved.getId(), saved.getVotingEndsAt());
        return saved;
    }

    public TournamentMatch closeMatchVoting(Long matchId, Long userId) {
        TournamentMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
        
        if (match.getStatus() != MatchStatus.VOTING) {
            throw new IllegalStateException("Can only close voting for VOTING matches");
        }
        
        Restaurant winner = match.determineWinner();
        match.setWinner(winner);
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());
        
        if (winner == null && match.getPhase() == TournamentPhase.GROUP_STAGE) {
            match.setIsDraw(true);
        }
        
        TournamentMatch saved = matchRepository.save(match);
        
        if (saved.getPhase() == TournamentPhase.GROUP_STAGE) {
            updateGroupPoints(saved);
        }
        
        publishEvent(
            EventType.MATCH_COMPLETED,
            "MATCH",
            saved.getId(),
            buildMatchPayload(saved)
        );
        
        log.info("Closed voting for match {}. Winner: {}", 
            saved.getId(), winner != null ? winner.getName() : "Draw");
        return saved;
    }

    private void updateGroupPoints(TournamentMatch match) {
        Long tournamentId = match.getTournament().getId();
        
        Restaurant restaurant1 = match.getRestaurant1();
        Restaurant restaurant2 = match.getRestaurant2();
        Restaurant winner = match.getWinner();
        
        Optional<ChallengeParticipation> p1Opt = participationRepository
            .findByTournamentIdAndRestaurantId(tournamentId, restaurant1.getId());
        Optional<ChallengeParticipation> p2Opt = participationRepository
            .findByTournamentIdAndRestaurantId(tournamentId, restaurant2.getId());
        
        if (p1Opt.isPresent() && p2Opt.isPresent()) {
            ChallengeParticipation p1 = p1Opt.get();
            ChallengeParticipation p2 = p2Opt.get();
            
            p1.setGroupMatchesPlayed(safeIncrement(p1.getGroupMatchesPlayed()));
            p2.setGroupMatchesPlayed(safeIncrement(p2.getGroupMatchesPlayed()));
            
            p1.setTotalVotes(safeAdd(p1.getTotalVotes(), match.getVotes1()));
            p2.setTotalVotes(safeAdd(p2.getTotalVotes(), match.getVotes2()));
            
            if (Boolean.TRUE.equals(match.getIsDraw())) {
                p1.setGroupDraws(safeIncrement(p1.getGroupDraws()));
                p2.setGroupDraws(safeIncrement(p2.getGroupDraws()));
                p1.setGroupPoints(safeAdd(p1.getGroupPoints(), 1));
                p2.setGroupPoints(safeAdd(p2.getGroupPoints(), 1));
            } else if (winner != null) {
                if (winner.getId().equals(restaurant1.getId())) {
                    p1.setGroupWins(safeIncrement(p1.getGroupWins()));
                    p1.setGroupPoints(safeAdd(p1.getGroupPoints(), 3));
                    p2.setGroupLosses(safeIncrement(p2.getGroupLosses()));
                } else {
                    p2.setGroupWins(safeIncrement(p2.getGroupWins()));
                    p2.setGroupPoints(safeAdd(p2.getGroupPoints(), 3));
                    p1.setGroupLosses(safeIncrement(p1.getGroupLosses()));
                }
            }
            
            participationRepository.save(p1);
            participationRepository.save(p2);
        }
    }

    @Transactional(readOnly = true)
    public List<TournamentMatch> getMatchesByTournament(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByPhaseAscMatchNumberAsc(tournamentId);
    }

    @Transactional(readOnly = true)
    public List<TournamentMatch> getMatchesByPhase(Long tournamentId, TournamentPhase phase) {
        return matchRepository.findByTournamentIdAndPhase(tournamentId, phase);
    }

    @Transactional(readOnly = true)
    public List<TournamentMatch> findExpiredVotingMatches() {
        return matchRepository.findExpiredVotingMatches(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<ChallengeParticipation> getGroupStandings(Long tournamentId, int groupNumber) {
        return participationRepository.findGroupStandings(tournamentId, groupNumber);
    }

    // ==================== PHASE ADVANCEMENT ====================

    public Tournament advanceToNextPhase(Long tournamentId, Long userId) {
        Tournament tournament = findById(tournamentId);
        TournamentPhase currentPhase = tournament.getCurrentPhase();
        
        long pendingMatches = matchRepository.countByTournamentAndStatus(
            tournamentId, MatchStatus.VOTING);
        long scheduledMatches = matchRepository.countByTournamentAndStatus(
            tournamentId, MatchStatus.SCHEDULED);
        
        if (pendingMatches > 0 || scheduledMatches > 0) {
            throw new IllegalStateException("Cannot advance: " + 
                (pendingMatches + scheduledMatches) + " matches still pending");
        }
        
        TournamentPhase nextPhase = getNextPhase(currentPhase, tournament.getParticipantCount());
        
        if (nextPhase == null) {
            return completeTournament(tournamentId, userId);
        }
        
        String oldPhase = currentPhase != null ? currentPhase.name() : null;
        tournament.setCurrentPhase(nextPhase);
        Tournament saved = tournamentRepository.save(tournament);
        
        if (currentPhase == TournamentPhase.GROUP_STAGE) {
            createKnockoutMatchesFromGroups(saved);
        } else {
            createNextKnockoutRound(saved, nextPhase);
        }
        
        auditService.auditTournamentPhaseChanged(
            saved.getId(),
            userId,
            ChallengeAuditLog.UserType.ADMIN,
            oldPhase,
            nextPhase.name()
        );
        
        publishEvent(
            EventType.TOURNAMENT_PHASE_CHANGED,
            "TOURNAMENT",
            saved.getId(),
            buildTournamentPayload(saved, null)
        );
        
        log.info("Tournament {} advanced to phase {}", saved.getId(), nextPhase);
        return saved;
    }

    private TournamentPhase getNextPhase(TournamentPhase current, int participantCount) {
        if (current == null) {
            return TournamentPhase.GROUP_STAGE;
        }
        
        return switch (current) {
            case GROUP_STAGE -> {
                if (participantCount >= 16) yield TournamentPhase.ROUND_OF_16;
                if (participantCount >= 8) yield TournamentPhase.QUARTER_FINALS;
                yield TournamentPhase.SEMI_FINALS;
            }
            case ROUND_OF_16 -> TournamentPhase.QUARTER_FINALS;
            case QUARTER_FINALS -> TournamentPhase.SEMI_FINALS;
            case SEMI_FINALS -> TournamentPhase.FINALS;
            case FINALS -> null;
            default -> null;
        };
    }

    private void createKnockoutMatchesFromGroups(Tournament tournament) {
        int qualifiersPerGroup = tournament.getQualifiersPerGroup() != null ? 
            tournament.getQualifiersPerGroup() : 2;
        
        List<Restaurant> qualifiedRestaurants = new ArrayList<>();
        
        for (int g = 1; g <= tournament.getGroupCount(); g++) {
            List<ChallengeParticipation> standings = getGroupStandings(tournament.getId(), g);
            
            for (int i = 0; i < Math.min(qualifiersPerGroup, standings.size()); i++) {
                ChallengeParticipation p = standings.get(i);
                p.setStatus(ParticipationStatus.QUALIFIED);
                p.setQualificationRank(i + 1);
                participationRepository.save(p);
                qualifiedRestaurants.add(p.getRestaurant());
            }
            
            for (int i = qualifiersPerGroup; i < standings.size(); i++) {
                ChallengeParticipation p = standings.get(i);
                p.setStatus(ParticipationStatus.ELIMINATED);
                participationRepository.save(p);
            }
        }
        
        createKnockoutBracket(tournament, qualifiedRestaurants, tournament.getCurrentPhase());
    }

    private void createKnockoutBracket(Tournament tournament, List<Restaurant> restaurants, 
                                       TournamentPhase phase) {
        Collections.shuffle(restaurants);
        
        int matchNumber = 0;
        for (int i = 0; i < restaurants.size() - 1; i += 2) {
            TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .phase(phase)
                .matchNumber(++matchNumber)
                .restaurant1(restaurants.get(i))
                .restaurant2(restaurants.get(i + 1))
                .status(MatchStatus.SCHEDULED)
                .votes1(0)
                .votes2(0)
                .build();
            
            matchRepository.save(match);
        }
        
        log.info("Created {} knockout matches for phase {}", matchNumber, phase);
    }

    private void createNextKnockoutRound(Tournament tournament, TournamentPhase phase) {
        TournamentPhase previousPhase = getPreviousPhase(phase);
        List<TournamentMatch> previousMatches = matchRepository
            .findByTournamentIdAndPhase(tournament.getId(), previousPhase);
        
        List<Restaurant> winners = previousMatches.stream()
            .filter(m -> m.getWinner() != null)
            .map(TournamentMatch::getWinner)
            .toList();
        
        if (winners.isEmpty()) {
            throw new IllegalStateException("No winners found from previous phase");
        }
        
        createKnockoutBracket(tournament, new ArrayList<>(winners), phase);
    }

    private TournamentPhase getPreviousPhase(TournamentPhase current) {
        return switch (current) {
            case QUARTER_FINALS -> TournamentPhase.ROUND_OF_16;
            case SEMI_FINALS -> TournamentPhase.QUARTER_FINALS;
            case FINALS -> TournamentPhase.SEMI_FINALS;
            default -> TournamentPhase.GROUP_STAGE;
        };
    }

    // ==================== STATISTICS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getTournamentStatistics(Long tournamentId) {
        Tournament tournament = findById(tournamentId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("tournamentId", tournamentId);
        stats.put("name", tournament.getName());
        stats.put("status", tournament.getStatus());
        stats.put("currentPhase", tournament.getCurrentPhase());
        stats.put("participantCount", tournament.getParticipantCount());
        
        long totalMatches = matchRepository.findByTournamentId(tournamentId).size();
        long completedMatches = matchRepository.countByTournamentAndStatus(tournamentId, MatchStatus.COMPLETED);
        Long totalVotes = matchRepository.getTotalVotesByTournament(tournamentId);
        
        stats.put("totalMatches", totalMatches);
        stats.put("completedMatches", completedMatches);
        stats.put("totalVotes", totalVotes != null ? totalVotes : 0);
        
        return stats;
    }

    // ==================== UTILITY ====================

    private void validateTournamentDates(Tournament tournament) {
        LocalDate today = LocalDate.now();
        
        if (tournament.getTournamentStart() != null && tournament.getTournamentStart().isBefore(today)) {
            throw new IllegalArgumentException("Tournament start date cannot be in the past");
        }
        
        if (tournament.getRegistrationStart() != null && tournament.getRegistrationEnd() != null) {
            if (tournament.getRegistrationEnd().isBefore(tournament.getRegistrationStart())) {
                throw new IllegalArgumentException("Registration end must be after registration start");
            }
        }
        
        if (tournament.getTournamentEnd() != null && tournament.getTournamentStart() != null) {
            if (tournament.getTournamentEnd().isBefore(tournament.getTournamentStart())) {
                throw new IllegalArgumentException("Tournament end must be after tournament start");
            }
        }
    }

    private Integer safeIncrement(Integer value) {
        return value != null ? value + 1 : 1;
    }

    private Integer safeAdd(Integer a, Integer b) {
        int va = a != null ? a : 0;
        int vb = b != null ? b : 0;
        return va + vb;
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

    private String buildTournamentPayload(Tournament tournament, Long userId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("tournamentId", tournament.getId());
            data.put("name", tournament.getName());
            data.put("status", tournament.getStatus().name());
            data.put("currentPhase", tournament.getCurrentPhase() != null ? 
                tournament.getCurrentPhase().name() : null);
            data.put("city", tournament.getCity());
            data.put("participantCount", tournament.getParticipantCount());
            if (userId != null) {
                data.put("userId", userId);
            }
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error building tournament payload", e);
            return "{}";
        }
    }

    private String buildMatchPayload(TournamentMatch match) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("matchId", match.getId());
            data.put("tournamentId", match.getTournament().getId());
            data.put("phase", match.getPhase().name());
            data.put("matchNumber", match.getMatchNumber());
            data.put("restaurant1Id", match.getRestaurant1().getId());
            data.put("restaurant1Name", match.getRestaurant1().getName());
            data.put("restaurant2Id", match.getRestaurant2().getId());
            data.put("restaurant2Name", match.getRestaurant2().getName());
            data.put("status", match.getStatus().name());
            data.put("votes1", match.getVotes1());
            data.put("votes2", match.getVotes2());
            if (match.getWinner() != null) {
                data.put("winnerId", match.getWinner().getId());
                data.put("winnerName", match.getWinner().getName());
            }
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error building match payload", e);
            return "{}";
        }
    }
}
