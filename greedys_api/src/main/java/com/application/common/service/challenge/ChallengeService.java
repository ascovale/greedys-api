package com.application.common.service.challenge;

import com.application.challenge.persistence.dao.ChallengeParticipationRepository;
import com.application.challenge.persistence.dao.ChallengeRepository;
import com.application.challenge.persistence.model.Challenge;
import com.application.challenge.persistence.model.ChallengeParticipation;
import com.application.challenge.persistence.model.enums.ChallengeStatus;
import com.application.challenge.persistence.model.enums.ChallengeType;
import com.application.challenge.persistence.model.enums.ParticipationStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;
    private final RestaurantDAO restaurantDAO;
    private final EventOutboxDAO eventOutboxDAO;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // ==================== CRUD ====================

    public Challenge createChallenge(Challenge challenge, Long userId) {
        log.info("Creating challenge: {}", challenge.getName());
        
        if (challenge.getStatus() == null) {
            challenge.setStatus(ChallengeStatus.DRAFT);
        }
        
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeCreated(
            saved.getId(),
            userId,
            ChallengeAuditLog.UserType.ADMIN,
            saved
        );
        
        publishEvent(
            EventType.CHALLENGE_CREATED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Created challenge {}: {}", saved.getId(), saved.getName());
        return saved;
    }

    public Challenge updateChallenge(Long challengeId, Challenge updates, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() == ChallengeStatus.COMPLETED ||
            challenge.getStatus() == ChallengeStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update challenge in status: " + challenge.getStatus());
        }
        
        if (updates.getName() != null) challenge.setName(updates.getName());
        if (updates.getDescription() != null) challenge.setDescription(updates.getDescription());
        if (updates.getFullDescription() != null) challenge.setFullDescription(updates.getFullDescription());
        if (updates.getCoverImageUrl() != null) challenge.setCoverImageUrl(updates.getCoverImageUrl());
        if (updates.getChallengeType() != null) challenge.setChallengeType(updates.getChallengeType());
        if (updates.getCategoryFilter() != null) challenge.setCategoryFilter(updates.getCategoryFilter());
        if (updates.getCity() != null) challenge.setCity(updates.getCity());
        if (updates.getRegion() != null) challenge.setRegion(updates.getRegion());
        if (updates.getStartDate() != null) challenge.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null) challenge.setEndDate(updates.getEndDate());
        if (updates.getMaxParticipants() != null) challenge.setMaxParticipants(updates.getMaxParticipants());
        
        Challenge saved = challengeRepository.save(challenge);
        log.info("Updated challenge {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Challenge findById(Long challengeId) {
        return challengeRepository.findById(challengeId)
            .orElseThrow(() -> new EntityNotFoundException("Challenge not found: " + challengeId));
    }

    @Transactional(readOnly = true)
    public Optional<Challenge> findBySlug(String slug) {
        return challengeRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public List<Challenge> findByStatus(ChallengeStatus status) {
        return challengeRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Page<Challenge> findByStatus(ChallengeStatus status, Pageable pageable) {
        return challengeRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Challenge> findByCity(String city) {
        return challengeRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Challenge> findByChallengeType(ChallengeType type) {
        return challengeRepository.findByChallengeType(type);
    }

    @Transactional(readOnly = true)
    public List<Challenge> findWithOpenRegistration() {
        return challengeRepository.findWithOpenRegistration(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Challenge> findWithOpenVoting() {
        return challengeRepository.findWithOpenVoting(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Challenge> findFeatured(List<ChallengeStatus> statuses) {
        return challengeRepository.findFeaturedWithStatuses(statuses);
    }

    @Transactional(readOnly = true)
    public Page<Challenge> search(String query, List<ChallengeStatus> statuses, Pageable pageable) {
        return challengeRepository.searchByQueryAndStatuses(query, statuses, pageable);
    }

    // ==================== LIFECYCLE ====================

    public Challenge publishChallenge(Long challengeId, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() != ChallengeStatus.DRAFT) {
            throw new IllegalStateException("Can only publish DRAFT challenges");
        }
        
        String oldStatus = challenge.getStatus().name();
        challenge.setStatus(ChallengeStatus.UPCOMING);
        challenge.setPublishedAt(LocalDateTime.now());
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeStatusChanged(
            challengeId, userId, ChallengeAuditLog.UserType.ADMIN,
            oldStatus, ChallengeStatus.UPCOMING.name()
        );
        
        publishEvent(
            EventType.CHALLENGE_STATUS_CHANGED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Published challenge {}", saved.getId());
        return saved;
    }

    public Challenge openRegistration(Long challengeId, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() != ChallengeStatus.UPCOMING) {
            throw new IllegalStateException("Can only open registration from UPCOMING status");
        }
        
        String oldStatus = challenge.getStatus().name();
        challenge.setStatus(ChallengeStatus.REGISTRATION);
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeStatusChanged(
            challengeId, userId, ChallengeAuditLog.UserType.ADMIN,
            oldStatus, ChallengeStatus.REGISTRATION.name()
        );
        
        publishEvent(
            EventType.CHALLENGE_REGISTRATION_OPENED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Opened registration for challenge {}", saved.getId());
        return saved;
    }

    public Challenge startChallenge(Long challengeId, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() != ChallengeStatus.REGISTRATION) {
            throw new IllegalStateException("Can only start from REGISTRATION status");
        }
        
        if (challenge.getParticipantsCount() < challenge.getMinParticipants()) {
            throw new IllegalStateException("Not enough participants to start challenge");
        }
        
        String oldStatus = challenge.getStatus().name();
        challenge.setStatus(ChallengeStatus.ACTIVE);
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeStatusChanged(
            challengeId, userId, ChallengeAuditLog.UserType.ADMIN,
            oldStatus, ChallengeStatus.ACTIVE.name()
        );
        
        publishEvent(
            EventType.CHALLENGE_STARTED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Started challenge {} with {} participants", saved.getId(), saved.getParticipantsCount());
        return saved;
    }

    public Challenge openVoting(Long challengeId, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
            throw new IllegalStateException("Can only open voting from ACTIVE status");
        }
        
        String oldStatus = challenge.getStatus().name();
        challenge.setStatus(ChallengeStatus.VOTING);
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeStatusChanged(
            challengeId, userId, ChallengeAuditLog.UserType.ADMIN,
            oldStatus, ChallengeStatus.VOTING.name()
        );
        
        publishEvent(
            EventType.CHALLENGE_VOTING_OPENED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Opened voting for challenge {}", saved.getId());
        return saved;
    }

    public Challenge completeChallenge(Long challengeId, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() != ChallengeStatus.VOTING) {
            throw new IllegalStateException("Can only complete from VOTING status");
        }
        
        String oldStatus = challenge.getStatus().name();
        challenge.setStatus(ChallengeStatus.COMPLETED);
        challenge.setCompletedAt(LocalDateTime.now());
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeStatusChanged(
            challengeId, userId, ChallengeAuditLog.UserType.ADMIN,
            oldStatus, ChallengeStatus.COMPLETED.name()
        );
        
        publishEvent(
            EventType.CHALLENGE_COMPLETED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Completed challenge {}", saved.getId());
        return saved;
    }

    public Challenge cancelChallenge(Long challengeId, String reason, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed challenge");
        }
        
        String oldStatus = challenge.getStatus().name();
        challenge.setStatus(ChallengeStatus.CANCELLED);
        Challenge saved = challengeRepository.save(challenge);
        
        auditService.auditChallengeStatusChanged(
            challengeId, userId, ChallengeAuditLog.UserType.ADMIN,
            oldStatus, ChallengeStatus.CANCELLED.name()
        );
        
        publishEvent(
            EventType.CHALLENGE_CANCELLED,
            "CHALLENGE",
            saved.getId(),
            buildChallengePayload(saved)
        );
        
        log.info("Cancelled challenge {}: {}", saved.getId(), reason);
        return saved;
    }

    // ==================== PARTICIPATION ====================

    public ChallengeParticipation registerRestaurant(Long challengeId, Long restaurantId, Long userId) {
        Challenge challenge = findById(challengeId);
        
        if (!challenge.canAcceptParticipants()) {
            throw new IllegalStateException("Challenge is not accepting participants");
        }
        
        if (participationRepository.existsByChallengeIdAndRestaurantId(challengeId, restaurantId)) {
            throw new IllegalStateException("Restaurant is already registered");
        }
        
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));
        
        ChallengeParticipation participation = ChallengeParticipation.builder()
            .challenge(challenge)
            .restaurant(restaurant)
            .status(ParticipationStatus.REGISTERED)
            .registeredAt(LocalDateTime.now())
            .build();
        
        ChallengeParticipation saved = participationRepository.save(participation);
        
        challenge.incrementParticipantsCount();
        challengeRepository.save(challenge);
        
        auditService.auditRestaurantRegistered(
            saved.getId(),
            challengeId,
            null,
            restaurantId,
            userId,
            ChallengeAuditLog.UserType.RESTAURANT_USER
        );
        
        publishEvent(
            EventType.CHALLENGE_RESTAURANT_REGISTERED,
            "CHALLENGE",
            challengeId,
            buildParticipationPayload(saved)
        );
        
        log.info("Restaurant {} registered for challenge {}", restaurantId, challengeId);
        return saved;
    }

    public void withdrawRestaurant(Long challengeId, Long restaurantId, String reason, Long userId) {
        ChallengeParticipation participation = participationRepository
            .findByChallengeIdAndRestaurantId(challengeId, restaurantId)
            .orElseThrow(() -> new EntityNotFoundException("Participation not found"));
        
        participation.setStatus(ParticipationStatus.WITHDRAWN);
        participation.setEliminationReason(reason);
        participation.setEliminatedAt(LocalDateTime.now());
        participationRepository.save(participation);
        
        Challenge challenge = findById(challengeId);
        challenge.decrementParticipantsCount();
        challengeRepository.save(challenge);
        
        log.info("Restaurant {} withdrawn from challenge {}", restaurantId, challengeId);
    }

    @Transactional(readOnly = true)
    public List<ChallengeParticipation> getParticipants(Long challengeId) {
        return participationRepository.findByChallengeId(challengeId);
    }

    @Transactional(readOnly = true)
    public Page<ChallengeParticipation> getParticipants(Long challengeId, Pageable pageable) {
        return participationRepository.findByChallengeId(challengeId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ChallengeParticipation> getParticipantsByScore(Long challengeId) {
        return participationRepository.findByChallengeOrderedByScore(challengeId);
    }

    // ==================== STATISTICS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getChallengeStatistics(Long challengeId) {
        Challenge challenge = findById(challengeId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("challengeId", challengeId);
        stats.put("name", challenge.getName());
        stats.put("status", challenge.getStatus());
        stats.put("participantsCount", challenge.getParticipantsCount());
        stats.put("totalVotes", challenge.getTotalVotes());
        stats.put("viewsCount", challenge.getViewsCount());
        stats.put("storiesCount", challenge.getStoriesCount());
        stats.put("remainingDays", challenge.getRemainingDays());
        
        return stats;
    }

    @Transactional(readOnly = true)
    public long countByStatus(ChallengeStatus status) {
        return challengeRepository.countByStatus(status);
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

    private String buildChallengePayload(Challenge challenge) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("challengeId", challenge.getId());
            data.put("name", challenge.getName());
            data.put("status", challenge.getStatus().name());
            data.put("challengeType", challenge.getChallengeType() != null ? challenge.getChallengeType().name() : null);
            data.put("city", challenge.getCity());
            data.put("participantsCount", challenge.getParticipantsCount());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error building challenge payload", e);
            return "{}";
        }
    }

    private String buildParticipationPayload(ChallengeParticipation participation) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("participationId", participation.getId());
            data.put("challengeId", participation.getChallenge().getId());
            data.put("restaurantId", participation.getRestaurant().getId());
            data.put("restaurantName", participation.getRestaurant().getName());
            data.put("status", participation.getStatus().name());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error building participation payload", e);
            return "{}";
        }
    }
}
