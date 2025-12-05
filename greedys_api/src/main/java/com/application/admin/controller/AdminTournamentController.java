package com.application.admin.controller;

import com.application.challenge.persistence.model.ChallengeParticipation;
import com.application.challenge.persistence.model.Tournament;
import com.application.challenge.persistence.model.TournamentMatch;
import com.application.challenge.persistence.model.enums.TournamentPhase;
import com.application.challenge.persistence.model.enums.TournamentStatus;
import com.application.common.service.challenge.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin Tournament Controller
 * Handles tournament management operations for administrators
 */
@RestController
@RequestMapping("/admin/tournaments")
@RequiredArgsConstructor
@Slf4j
public class AdminTournamentController {

    private final TournamentService tournamentService;

    // ==================== CRUD ====================

    /**
     * Create a new tournament
     */
    @PostMapping
    public ResponseEntity<Tournament> createTournament(
            @RequestBody CreateTournamentRequest request,
            Principal principal) {
        log.info("Admin creating tournament: {}", request.name());
        
        Tournament tournament = Tournament.builder()
                .name(request.name())
                .description(request.description())
                .rules(request.rules())
                .city(request.city())
                .zone(request.zone())
                .cuisineType(request.cuisineType())
                .dishCategory(request.dishCategory())
                .registrationStart(request.registrationStart())
                .registrationEnd(request.registrationEnd())
                .tournamentStart(request.tournamentStart())
                .tournamentEnd(request.tournamentEnd())
                .maxParticipants(request.maxParticipants())
                .groupCount(request.groupCount())
                .groupSize(request.groupSize())
                .qualifiersPerGroup(request.qualifiersPerGroup())
                .matchVotingHours(request.matchVotingHours())
                .firstPrizeBadge(request.firstPrizeBadge())
                .secondPrizeBadge(request.secondPrizeBadge())
                .thirdPrizeBadge(request.thirdPrizeBadge())
                .firstPrizeDescription(request.firstPrizeDescription())
                .build();
        
        Long userId = extractUserId(principal);
        Tournament saved = tournamentService.createTournament(tournament, userId);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update an existing tournament
     */
    @PutMapping("/{tournamentId}")
    public ResponseEntity<Tournament> updateTournament(
            @PathVariable Long tournamentId,
            @RequestBody UpdateTournamentRequest request,
            Principal principal) {
        log.info("Admin updating tournament: {}", tournamentId);
        
        Tournament updates = Tournament.builder()
                .name(request.name())
                .description(request.description())
                .rules(request.rules())
                .city(request.city())
                .zone(request.zone())
                .cuisineType(request.cuisineType())
                .maxParticipants(request.maxParticipants())
                .matchVotingHours(request.matchVotingHours())
                .build();
        
        Long userId = extractUserId(principal);
        Tournament saved = tournamentService.updateTournament(tournamentId, updates, userId);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all tournaments
     */
    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments(
            @RequestParam(required = false) TournamentStatus status) {
        log.debug("Admin fetching tournaments, status={}", status);
        
        List<Tournament> tournaments;
        if (status != null) {
            tournaments = tournamentService.findByStatus(status);
        } else {
            // Get all - combine multiple statuses
            tournaments = new java.util.ArrayList<>();
            for (TournamentStatus s : TournamentStatus.values()) {
                tournaments.addAll(tournamentService.findByStatus(s));
            }
        }
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Get tournament by ID
     */
    @GetMapping("/{tournamentId}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable Long tournamentId) {
        log.debug("Admin fetching tournament: {}", tournamentId);
        Tournament tournament = tournamentService.findById(tournamentId);
        return ResponseEntity.ok(tournament);
    }

    // ==================== LIFECYCLE ====================

    /**
     * Open registration for a tournament
     */
    @PostMapping("/{tournamentId}/open-registration")
    public ResponseEntity<Tournament> openRegistration(
            @PathVariable Long tournamentId,
            Principal principal) {
        log.info("Admin opening registration for tournament: {}", tournamentId);
        Long userId = extractUserId(principal);
        Tournament tournament = tournamentService.openRegistration(tournamentId, userId);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Start a tournament
     */
    @PostMapping("/{tournamentId}/start")
    public ResponseEntity<Tournament> startTournament(
            @PathVariable Long tournamentId,
            Principal principal) {
        log.info("Admin starting tournament: {}", tournamentId);
        Long userId = extractUserId(principal);
        Tournament tournament = tournamentService.startTournament(tournamentId, userId);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Advance tournament to next phase
     */
    @PostMapping("/{tournamentId}/advance-phase")
    public ResponseEntity<Tournament> advanceToNextPhase(
            @PathVariable Long tournamentId,
            Principal principal) {
        log.info("Admin advancing tournament {} to next phase", tournamentId);
        Long userId = extractUserId(principal);
        Tournament tournament = tournamentService.advanceToNextPhase(tournamentId, userId);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Complete a tournament
     */
    @PostMapping("/{tournamentId}/complete")
    public ResponseEntity<Tournament> completeTournament(
            @PathVariable Long tournamentId,
            Principal principal) {
        log.info("Admin completing tournament: {}", tournamentId);
        Long userId = extractUserId(principal);
        Tournament tournament = tournamentService.completeTournament(tournamentId, userId);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Cancel a tournament
     */
    @PostMapping("/{tournamentId}/cancel")
    public ResponseEntity<Tournament> cancelTournament(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) String reason,
            Principal principal) {
        log.info("Admin cancelling tournament: {} reason: {}", tournamentId, reason);
        Long userId = extractUserId(principal);
        Tournament tournament = tournamentService.cancelTournament(tournamentId, reason, userId);
        return ResponseEntity.ok(tournament);
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Get tournament participants
     */
    @GetMapping("/{tournamentId}/participants")
    public ResponseEntity<List<ChallengeParticipation>> getParticipants(@PathVariable Long tournamentId) {
        log.debug("Admin fetching participants for tournament: {}", tournamentId);
        List<ChallengeParticipation> participants = tournamentService.getParticipants(tournamentId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Get group standings
     */
    @GetMapping("/{tournamentId}/group/{groupNumber}/standings")
    public ResponseEntity<List<ChallengeParticipation>> getGroupStandings(
            @PathVariable Long tournamentId,
            @PathVariable int groupNumber) {
        log.debug("Admin fetching standings for tournament {} group {}", tournamentId, groupNumber);
        List<ChallengeParticipation> standings = tournamentService.getGroupStandings(tournamentId, groupNumber);
        return ResponseEntity.ok(standings);
    }

    // ==================== MATCHES ====================

    /**
     * Get tournament matches
     */
    @GetMapping("/{tournamentId}/matches")
    public ResponseEntity<List<TournamentMatch>> getMatches(@PathVariable Long tournamentId) {
        log.debug("Admin fetching matches for tournament: {}", tournamentId);
        List<TournamentMatch> matches = tournamentService.getMatchesByTournament(tournamentId);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get matches by phase
     */
    @GetMapping("/{tournamentId}/matches/phase/{phase}")
    public ResponseEntity<List<TournamentMatch>> getMatchesByPhase(
            @PathVariable Long tournamentId,
            @PathVariable TournamentPhase phase) {
        log.debug("Admin fetching matches for tournament {} phase {}", tournamentId, phase);
        List<TournamentMatch> matches = tournamentService.getMatchesByPhase(tournamentId, phase);
        return ResponseEntity.ok(matches);
    }

    /**
     * Create a match manually
     */
    @PostMapping("/{tournamentId}/matches")
    public ResponseEntity<TournamentMatch> createMatch(
            @PathVariable Long tournamentId,
            @RequestBody CreateMatchRequest request,
            Principal principal) {
        log.info("Admin creating match for tournament: {}", tournamentId);
        Long userId = extractUserId(principal);
        TournamentMatch match = tournamentService.createMatch(
                tournamentId,
                request.phase(),
                request.restaurant1Id(),
                request.restaurant2Id(),
                request.matchNumber(),
                userId
        );
        return ResponseEntity.ok(match);
    }

    /**
     * Start voting for a match
     */
    @PostMapping("/matches/{matchId}/start-voting")
    public ResponseEntity<TournamentMatch> startMatchVoting(
            @PathVariable Long matchId,
            Principal principal) {
        log.info("Admin starting voting for match: {}", matchId);
        Long userId = extractUserId(principal);
        TournamentMatch match = tournamentService.startMatchVoting(matchId, userId);
        return ResponseEntity.ok(match);
    }

    /**
     * Close voting for a match
     */
    @PostMapping("/matches/{matchId}/close-voting")
    public ResponseEntity<TournamentMatch> closeMatchVoting(
            @PathVariable Long matchId,
            Principal principal) {
        log.info("Admin closing voting for match: {}", matchId);
        Long userId = extractUserId(principal);
        TournamentMatch match = tournamentService.closeMatchVoting(matchId, userId);
        return ResponseEntity.ok(match);
    }

    /**
     * Get expired voting matches (for manual cleanup)
     */
    @GetMapping("/matches/expired-voting")
    public ResponseEntity<List<TournamentMatch>> getExpiredVotingMatches() {
        log.debug("Admin fetching expired voting matches");
        List<TournamentMatch> matches = tournamentService.findExpiredVotingMatches();
        return ResponseEntity.ok(matches);
    }

    // ==================== STATISTICS ====================

    /**
     * Get tournament statistics
     */
    @GetMapping("/{tournamentId}/statistics")
    public ResponseEntity<Map<String, Object>> getTournamentStatistics(@PathVariable Long tournamentId) {
        log.debug("Admin fetching statistics for tournament: {}", tournamentId);
        Map<String, Object> stats = tournamentService.getTournamentStatistics(tournamentId);
        return ResponseEntity.ok(stats);
    }

    // ==================== UTILITY ====================

    private Long extractUserId(Principal principal) {
        return 1L; // Placeholder
    }

    // ==================== DTOs ====================

    public record CreateTournamentRequest(
            String name,
            String description,
            String rules,
            String city,
            String zone,
            String cuisineType,
            String dishCategory,
            LocalDate registrationStart,
            LocalDate registrationEnd,
            LocalDate tournamentStart,
            LocalDate tournamentEnd,
            Integer maxParticipants,
            Integer groupCount,
            Integer groupSize,
            Integer qualifiersPerGroup,
            Integer matchVotingHours,
            String firstPrizeBadge,
            String secondPrizeBadge,
            String thirdPrizeBadge,
            String firstPrizeDescription
    ) {}

    public record UpdateTournamentRequest(
            String name,
            String description,
            String rules,
            String city,
            String zone,
            String cuisineType,
            Integer maxParticipants,
            Integer matchVotingHours
    ) {}

    public record CreateMatchRequest(
            TournamentPhase phase,
            Long restaurant1Id,
            Long restaurant2Id,
            int matchNumber
    ) {}
}
