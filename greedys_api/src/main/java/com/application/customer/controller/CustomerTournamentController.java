package com.application.customer.controller;

import com.application.challenge.persistence.model.Tournament;
import com.application.challenge.persistence.model.TournamentMatch;
import com.application.challenge.persistence.model.enums.TournamentPhase;
import com.application.challenge.persistence.model.enums.TournamentStatus;
import com.application.common.service.challenge.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer Tournament Controller
 * Handles tournament operations for customers (view tournaments, matches)
 */
@RestController
@RequestMapping("/customer/tournaments")
@RequiredArgsConstructor
@Slf4j
public class CustomerTournamentController {

    private final TournamentService tournamentService;

    // ==================== TOURNAMENT DISCOVERY ====================

    /**
     * Get active tournaments
     */
    @GetMapping
    public ResponseEntity<List<Tournament>> getActiveTournaments() {
        log.debug("Fetching active tournaments");
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

    // ==================== BRACKET ====================

    /**
     * Get tournament bracket (all matches organized by phase)
     */
    @GetMapping("/{tournamentId}/bracket")
    public ResponseEntity<TournamentBracketResponse> getTournamentBracket(@PathVariable Long tournamentId) {
        log.debug("Fetching bracket for tournament: {}", tournamentId);
        Tournament tournament = tournamentService.findById(tournamentId);
        List<TournamentMatch> matches = tournamentService.getMatchesByTournament(tournamentId);
        return ResponseEntity.ok(new TournamentBracketResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getCurrentPhase(),
                matches
        ));
    }

    /**
     * Get tournament statistics
     */
    @GetMapping("/{tournamentId}/statistics")
    public ResponseEntity<?> getTournamentStatistics(@PathVariable Long tournamentId) {
        log.debug("Fetching statistics for tournament: {}", tournamentId);
        return ResponseEntity.ok(tournamentService.getTournamentStatistics(tournamentId));
    }

    // ==================== DTOs ====================

    public record TournamentBracketResponse(
            Long tournamentId,
            String tournamentName,
            TournamentPhase currentPhase,
            List<TournamentMatch> matches
    ) {}
}
