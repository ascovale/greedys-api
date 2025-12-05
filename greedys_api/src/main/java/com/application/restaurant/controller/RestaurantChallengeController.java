package com.application.restaurant.controller;

import com.application.challenge.persistence.model.Challenge;
import com.application.challenge.persistence.model.ChallengeParticipation;
import com.application.challenge.persistence.model.enums.ChallengeStatus;
import com.application.common.service.challenge.ChallengeService;
import com.application.restaurant.persistence.model.user.RUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Restaurant Challenge Controller
 * Handles challenge operations for restaurants (view challenges, participate, withdraw)
 */
@RestController
@RequestMapping("/restaurant/challenges")
@RequiredArgsConstructor
@Slf4j
public class RestaurantChallengeController {

    private final ChallengeService challengeService;

    // ==================== CHALLENGE DISCOVERY ====================

    /**
     * Get active challenges available to restaurants
     */
    @GetMapping
    public ResponseEntity<Page<Challenge>> getActiveChallenges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Fetching active challenges for restaurants");
        Page<Challenge> challenges = challengeService.findByStatus(ChallengeStatus.ACTIVE, PageRequest.of(page, size));
        return ResponseEntity.ok(challenges);
    }

    /**
     * Get challenges with open registration
     */
    @GetMapping("/open-registration")
    public ResponseEntity<List<Challenge>> getChallengesWithOpenRegistration() {
        log.debug("Fetching challenges with open registration");
        List<Challenge> challenges = challengeService.findWithOpenRegistration();
        return ResponseEntity.ok(challenges);
    }

    /**
     * Get challenges with open voting
     */
    @GetMapping("/open-voting")
    public ResponseEntity<List<Challenge>> getChallengesWithOpenVoting() {
        log.debug("Fetching challenges with open voting");
        List<Challenge> challenges = challengeService.findWithOpenVoting();
        return ResponseEntity.ok(challenges);
    }

    /**
     * Get challenge details by ID
     */
    @GetMapping("/{challengeId}")
    public ResponseEntity<Challenge> getChallengeDetails(@PathVariable Long challengeId) {
        log.debug("Fetching challenge details: {}", challengeId);
        Challenge challenge = challengeService.findById(challengeId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Get challenges by city
     */
    @GetMapping("/by-city/{city}")
    public ResponseEntity<List<Challenge>> getChallengesByCity(@PathVariable String city) {
        log.debug("Fetching challenges for city: {}", city);
        List<Challenge> challenges = challengeService.findByCity(city);
        return ResponseEntity.ok(challenges);
    }

    // ==================== PARTICIPATION ====================

    /**
     * Register restaurant for a challenge
     */
    @PostMapping("/{challengeId}/register")
    public ResponseEntity<ChallengeParticipation> registerForChallenge(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long challengeId) {
        log.info("Restaurant {} registering for challenge: {}", 
                restaurantUser.getRestaurant().getId(), challengeId);
        ChallengeParticipation participation = challengeService.registerRestaurant(
                challengeId, 
                restaurantUser.getRestaurant().getId(),
                restaurantUser.getId()
        );
        return ResponseEntity.ok(participation);
    }

    /**
     * Withdraw restaurant from a challenge
     */
    @PostMapping("/{challengeId}/withdraw")
    public ResponseEntity<Void> withdrawFromChallenge(
            @AuthenticationPrincipal RUser restaurantUser,
            @PathVariable Long challengeId,
            @RequestParam(required = false) String reason) {
        log.info("Restaurant {} withdrawing from challenge: {}", 
                restaurantUser.getRestaurant().getId(), challengeId);
        challengeService.withdrawRestaurant(
                challengeId, 
                restaurantUser.getRestaurant().getId(),
                reason,
                restaurantUser.getId()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Get participants of a challenge
     */
    @GetMapping("/{challengeId}/participants")
    public ResponseEntity<List<ChallengeParticipation>> getChallengeParticipants(
            @PathVariable Long challengeId) {
        log.debug("Fetching participants for challenge: {}", challengeId);
        List<ChallengeParticipation> participants = challengeService.getParticipants(challengeId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Get participants ordered by score
     */
    @GetMapping("/{challengeId}/leaderboard")
    public ResponseEntity<List<ChallengeParticipation>> getChallengeLeaderboard(
            @PathVariable Long challengeId) {
        log.debug("Fetching leaderboard for challenge: {}", challengeId);
        List<ChallengeParticipation> leaderboard = challengeService.getParticipantsByScore(challengeId);
        return ResponseEntity.ok(leaderboard);
    }

    // ==================== STATISTICS ====================

    /**
     * Get challenge statistics
     */
    @GetMapping("/{challengeId}/statistics")
    public ResponseEntity<Map<String, Object>> getChallengeStatistics(@PathVariable Long challengeId) {
        log.debug("Fetching statistics for challenge: {}", challengeId);
        Map<String, Object> stats = challengeService.getChallengeStatistics(challengeId);
        return ResponseEntity.ok(stats);
    }
}
