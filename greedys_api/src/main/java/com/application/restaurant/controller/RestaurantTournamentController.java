package com.application.restaurant.controller;

import com.application.challenge.persistence.model.ChallengeParticipation;
import com.application.challenge.persistence.model.Tournament;
import com.application.challenge.persistence.model.TournamentMatch;
import com.application.challenge.persistence.model.enums.TournamentPhase;
import com.application.challenge.persistence.model.enums.TournamentStatus;
import com.application.common.service.challenge.TournamentService;
import com.application.restaurant.persistence.model.user.RUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Restaurant Tournament Controller
 * Handles tournament operations for restaurants (view, register, matches)
 */
@RestController
@RequestMapping("/restaurant/tournaments")
@RequiredArgsConstructor
@Slf4j
public class RestaurantTournamentController {

    private final TournamentService tournamentService;

    // ==================== TOURNAMENT DISCOVERY ====================

    /**
     * Get active tournaments
     */
    @GetMapping
    public ResponseEntity<List<Tournament>> getActiveTournaments() {
        log.debug("Fetching active tournaments for restaurants");
        List<Tournament> tournaments = tournamentService.findByStatus(TournamentStatus.ONGOING);
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Get tournaments with open registration
     */
    @GetMapping("/open-registration")
    public ResponseEntity<List<Tournament>> getTournamentsWithOpenRegistration() {
        log.debug("Fetching tournaments with open registration");
        List<Tournament> tournaments = tournamentService.findWithOpenRegistration();
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Get tournament details by ID
     */
    @GetMapping("/{tournamentId}")
    public ResponseEntity<Tournament> getTournamentDetails(@PathVariable Long tournamentId) {
        log.debug("Fetching tournament details: {}", tournamentId);
        Tournament tournament = tournamentService.findById(tournamentId);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Get tournaments by city
     */
    @GetMapping("/by-city/{city}")
    public ResponseEntity<List<Tournament>> getTournamentsByCity(@PathVariable String city) {
        log.debug("Fetching tournaments for city: {}", city);
        List<Tournament> tournaments = tournamentService.findByCity(city);
        return ResponseEntity.ok(tournaments);
    }

    // ==================== REGISTRATION ====================

    /**
     * Register restaurant for a tournament
     */
    @PostMapping("/{tournamentId}/register")
    public ResponseEntity<ChallengeParticipation> registerForTournament(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long tournamentId) {
        log.info("Restaurant {} registering for tournament: {}", 
                restaurantUser.getRestaurant().getId(), tournamentId);
        ChallengeParticipation participation = tournamentService.registerRestaurant(
                tournamentId, 
                restaurantUser.getRestaurant().getId(),
                restaurantUser.getId()
        );
        return ResponseEntity.ok(participation);
    }

    /**
     * Withdraw restaurant from a tournament
     */
    @PostMapping("/{tournamentId}/withdraw")
    public ResponseEntity<Void> withdrawFromTournament(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long tournamentId,
            @RequestParam(required = false) String reason) {
        log.info("Restaurant {} withdrawing from tournament: {}", 
                restaurantUser.getRestaurant().getId(), tournamentId);
        tournamentService.withdrawRestaurant(
                tournamentId, 
                restaurantUser.getRestaurant().getId(),
                reason,
                restaurantUser.getId()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Get participants of a tournament
     */
    @GetMapping("/{tournamentId}/participants")
    public ResponseEntity<List<ChallengeParticipation>> getTournamentParticipants(
            @PathVariable Long tournamentId) {
        log.debug("Fetching participants for tournament: {}", tournamentId);
        List<ChallengeParticipation> participants = tournamentService.getParticipants(tournamentId);
        return ResponseEntity.ok(participants);
    }

    // ==================== MATCHES ====================

    /**
     * Get matches for a tournament
     */
    @GetMapping("/{tournamentId}/matches")
    public ResponseEntity<List<TournamentMatch>> getTournamentMatches(@PathVariable Long tournamentId) {
        log.debug("Fetching matches for tournament: {}", tournamentId);
        List<TournamentMatch> matches = tournamentService.getMatchesByTournament(tournamentId);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get matches for a specific phase
     */
    @GetMapping("/{tournamentId}/matches/phase/{phase}")
    public ResponseEntity<List<TournamentMatch>> getMatchesByPhase(
            @PathVariable Long tournamentId,
            @PathVariable TournamentPhase phase) {
        log.debug("Fetching matches for tournament {} phase {}", tournamentId, phase);
        List<TournamentMatch> matches = tournamentService.getMatchesByPhase(tournamentId, phase);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get group standings
     */
    @GetMapping("/{tournamentId}/group/{groupNumber}/standings")
    public ResponseEntity<List<ChallengeParticipation>> getGroupStandings(
            @PathVariable Long tournamentId,
            @PathVariable int groupNumber) {
        log.debug("Fetching standings for tournament {} group {}", tournamentId, groupNumber);
        List<ChallengeParticipation> standings = tournamentService.getGroupStandings(tournamentId, groupNumber);
        return ResponseEntity.ok(standings);
    }

    // ==================== STATISTICS ====================

    /**
     * Get tournament statistics
     */
    @GetMapping("/{tournamentId}/statistics")
    public ResponseEntity<Map<String, Object>> getTournamentStatistics(@PathVariable Long tournamentId) {
        log.debug("Fetching statistics for tournament: {}", tournamentId);
        Map<String, Object> stats = tournamentService.getTournamentStatistics(tournamentId);
        return ResponseEntity.ok(stats);
    }
}
