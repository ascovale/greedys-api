package com.application.admin.controller;

import com.application.challenge.persistence.model.Challenge;
import com.application.challenge.persistence.model.ChallengeParticipation;
import com.application.challenge.persistence.model.enums.ChallengeStatus;
import com.application.challenge.persistence.model.enums.ChallengeType;
import com.application.common.service.challenge.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin Challenge Controller
 * Handles challenge management operations for administrators
 */
@RestController
@RequestMapping("/admin/challenges")
@RequiredArgsConstructor
@Slf4j
public class AdminChallengeController {

    private final ChallengeService challengeService;

    // ==================== CRUD ====================

    /**
     * Create a new challenge
     */
    @PostMapping
    public ResponseEntity<Challenge> createChallenge(
            @RequestBody CreateChallengeRequest request,
            Principal principal) {
        log.info("Admin creating challenge: {}", request.name());
        
        Challenge challenge = Challenge.builder()
                .name(request.name())
                .description(request.description())
                .fullDescription(request.fullDescription())
                .coverImageUrl(request.coverImageUrl())
                .challengeType(request.challengeType())
                .categoryFilter(request.categoryFilter())
                .city(request.city())
                .region(request.region())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .registrationStartDate(request.registrationStartDate())
                .registrationEndDate(request.registrationEndDate())
                .votingStartDate(request.votingStartDate())
                .votingEndDate(request.votingEndDate())
                .maxParticipants(request.maxParticipants())
                .minParticipants(request.minParticipants())
                .build();
        
        Long userId = extractUserId(principal);
        Challenge saved = challengeService.createChallenge(challenge, userId);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update an existing challenge
     */
    @PutMapping("/{challengeId}")
    public ResponseEntity<Challenge> updateChallenge(
            @PathVariable Long challengeId,
            @RequestBody UpdateChallengeRequest request,
            Principal principal) {
        log.info("Admin updating challenge: {}", challengeId);
        
        Challenge updates = Challenge.builder()
                .name(request.name())
                .description(request.description())
                .fullDescription(request.fullDescription())
                .coverImageUrl(request.coverImageUrl())
                .categoryFilter(request.categoryFilter())
                .city(request.city())
                .region(request.region())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .maxParticipants(request.maxParticipants())
                .build();
        
        Long userId = extractUserId(principal);
        Challenge saved = challengeService.updateChallenge(challengeId, updates, userId);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all challenges (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<Challenge>> getAllChallenges(
            @RequestParam(required = false) ChallengeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Admin fetching challenges, status={}", status);
        
        Page<Challenge> challenges;
        if (status != null) {
            challenges = challengeService.findByStatus(status, PageRequest.of(page, size));
        } else {
            // Get all by searching with empty query
            challenges = challengeService.search("", List.of(ChallengeStatus.values()), PageRequest.of(page, size));
        }
        return ResponseEntity.ok(challenges);
    }

    /**
     * Get challenge by ID
     */
    @GetMapping("/{challengeId}")
    public ResponseEntity<Challenge> getChallengeById(@PathVariable Long challengeId) {
        log.debug("Admin fetching challenge: {}", challengeId);
        Challenge challenge = challengeService.findById(challengeId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Get challenges by status
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<Challenge>> getChallengesByStatus(@PathVariable ChallengeStatus status) {
        log.debug("Admin fetching challenges by status: {}", status);
        List<Challenge> challenges = challengeService.findByStatus(status);
        return ResponseEntity.ok(challenges);
    }

    /**
     * Get challenges by type
     */
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<Challenge>> getChallengesByType(@PathVariable ChallengeType type) {
        log.debug("Admin fetching challenges by type: {}", type);
        List<Challenge> challenges = challengeService.findByChallengeType(type);
        return ResponseEntity.ok(challenges);
    }

    // ==================== LIFECYCLE ====================

    /**
     * Publish a draft challenge
     */
    @PostMapping("/{challengeId}/publish")
    public ResponseEntity<Challenge> publishChallenge(
            @PathVariable Long challengeId,
            Principal principal) {
        log.info("Admin publishing challenge: {}", challengeId);
        Long userId = extractUserId(principal);
        Challenge challenge = challengeService.publishChallenge(challengeId, userId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Open registration for a challenge
     */
    @PostMapping("/{challengeId}/open-registration")
    public ResponseEntity<Challenge> openRegistration(
            @PathVariable Long challengeId,
            Principal principal) {
        log.info("Admin opening registration for challenge: {}", challengeId);
        Long userId = extractUserId(principal);
        Challenge challenge = challengeService.openRegistration(challengeId, userId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Start a challenge
     */
    @PostMapping("/{challengeId}/start")
    public ResponseEntity<Challenge> startChallenge(
            @PathVariable Long challengeId,
            Principal principal) {
        log.info("Admin starting challenge: {}", challengeId);
        Long userId = extractUserId(principal);
        Challenge challenge = challengeService.startChallenge(challengeId, userId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Open voting for a challenge
     */
    @PostMapping("/{challengeId}/open-voting")
    public ResponseEntity<Challenge> openVoting(
            @PathVariable Long challengeId,
            Principal principal) {
        log.info("Admin opening voting for challenge: {}", challengeId);
        Long userId = extractUserId(principal);
        Challenge challenge = challengeService.openVoting(challengeId, userId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Complete a challenge
     */
    @PostMapping("/{challengeId}/complete")
    public ResponseEntity<Challenge> completeChallenge(
            @PathVariable Long challengeId,
            Principal principal) {
        log.info("Admin completing challenge: {}", challengeId);
        Long userId = extractUserId(principal);
        Challenge challenge = challengeService.completeChallenge(challengeId, userId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Cancel a challenge
     */
    @PostMapping("/{challengeId}/cancel")
    public ResponseEntity<Challenge> cancelChallenge(
            @PathVariable Long challengeId,
            @RequestParam(required = false) String reason,
            Principal principal) {
        log.info("Admin cancelling challenge: {} reason: {}", challengeId, reason);
        Long userId = extractUserId(principal);
        Challenge challenge = challengeService.cancelChallenge(challengeId, reason, userId);
        return ResponseEntity.ok(challenge);
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Get challenge participants
     */
    @GetMapping("/{challengeId}/participants")
    public ResponseEntity<List<ChallengeParticipation>> getParticipants(@PathVariable Long challengeId) {
        log.debug("Admin fetching participants for challenge: {}", challengeId);
        List<ChallengeParticipation> participants = challengeService.getParticipants(challengeId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Get challenge participants paginated
     */
    @GetMapping("/{challengeId}/participants/page")
    public ResponseEntity<Page<ChallengeParticipation>> getParticipantsPaged(
            @PathVariable Long challengeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Admin fetching paginated participants for challenge: {}", challengeId);
        Page<ChallengeParticipation> participants = challengeService.getParticipants(challengeId, PageRequest.of(page, size));
        return ResponseEntity.ok(participants);
    }

    /**
     * Get leaderboard
     */
    @GetMapping("/{challengeId}/leaderboard")
    public ResponseEntity<List<ChallengeParticipation>> getLeaderboard(@PathVariable Long challengeId) {
        log.debug("Admin fetching leaderboard for challenge: {}", challengeId);
        List<ChallengeParticipation> leaderboard = challengeService.getParticipantsByScore(challengeId);
        return ResponseEntity.ok(leaderboard);
    }

    // ==================== STATISTICS ====================

    /**
     * Get challenge statistics
     */
    @GetMapping("/{challengeId}/statistics")
    public ResponseEntity<Map<String, Object>> getChallengeStatistics(@PathVariable Long challengeId) {
        log.debug("Admin fetching statistics for challenge: {}", challengeId);
        Map<String, Object> stats = challengeService.getChallengeStatistics(challengeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get count by status
     */
    @GetMapping("/count/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable ChallengeStatus status) {
        log.debug("Admin counting challenges by status: {}", status);
        long count = challengeService.countByStatus(status);
        return ResponseEntity.ok(count);
    }

    // ==================== UTILITY ====================

    private Long extractUserId(Principal principal) {
        // Extract user ID from principal - implementation depends on auth system
        return 1L; // Placeholder
    }

    // ==================== DTOs ====================

    public record CreateChallengeRequest(
            String name,
            String description,
            String fullDescription,
            String coverImageUrl,
            ChallengeType challengeType,
            String categoryFilter,
            String city,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationStartDate,
            LocalDate registrationEndDate,
            LocalDate votingStartDate,
            LocalDate votingEndDate,
            Integer maxParticipants,
            Integer minParticipants
    ) {}

    public record UpdateChallengeRequest(
            String name,
            String description,
            String fullDescription,
            String coverImageUrl,
            String categoryFilter,
            String city,
            String region,
            LocalDate startDate,
            LocalDate endDate,
            Integer maxParticipants
    ) {}
}
