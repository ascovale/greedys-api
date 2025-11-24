package com.application.common.service.notification.orchestrator;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.application.common.persistence.model.notification.ANotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ ORCHESTRATOR FACTORY - Dispatch to Type-Specific Orchestrator
 * 
 * Single entry point for getting the correct orchestrator based on user type.
 * 
 * USER TYPES:
 * 1. RESTAURANT → RestaurantUserOrchestrator
 * 2. RESTAURANT_TEAM → RestaurantTeamOrchestrator (TEAM scope for reservations)
 * 3. CUSTOMER → CustomerOrchestrator
 * 4. AGENCY → AgencyUserOrchestrator
 * 5. ADMIN → AdminOrchestrator
 * 
 * USAGE:
 * NotificationOrchestrator<RestaurantUserNotification> orchestrator
 *   = factory.getOrchestrator(RESTAURANT);
 * 
 * List<RestaurantUserNotification> disaggregated
 *   = orchestrator.disaggregateAndProcess(message);
 * 
 * @author Greedy's System
 * @since 2025-01-21 (New factory pattern for orchestrator dispatch)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationOrchestratorFactory {

    // Inject all 4 user-type-specific orchestrators
    private final RestaurantUserOrchestrator restaurantOrchestrator;
    private final RestaurantTeamOrchestrator restaurantTeamOrchestrator;
    private final CustomerOrchestrator customerOrchestrator;
    private final AgencyUserOrchestrator agencyOrchestrator;
    private final AdminOrchestrator adminOrchestrator;

    /**
     * Gets the orchestrator for a specific user type.
     * 
     * User type can be provided in multiple ways:
     * 1. Via message.aggregateType (recommended)
     * 2. Via direct UserType parameter
     * 
     * @param message RabbitMQ message (contains aggregateType field)
     * @return NotificationOrchestrator for the user type
     */
    public <T extends ANotification> NotificationOrchestrator<T> getOrchestratorFromMessage(
        Map<String, Object> message
    ) {
        String aggregateType = (String) message.get("aggregate_type");
        return getOrchestrator(aggregateType);
    }

    /**
     * Gets the orchestrator for a specific user type.
     * 
     * @param userType User type enum or string (RESTAURANT, CUSTOMER, AGENCY, ADMIN)
     * @return NotificationOrchestrator for the user type
     */
    @SuppressWarnings("unchecked")
    public <T extends ANotification> NotificationOrchestrator<T> getOrchestrator(String userType) {
        if (userType == null) {
            throw new IllegalArgumentException("User type cannot be null");
        }

        NotificationOrchestrator<?> orchestrator = switch (userType.toUpperCase()) {
            case "RESTAURANT" -> {
                log.debug("🏢 Returning RestaurantUserOrchestrator for type RESTAURANT");
                yield restaurantOrchestrator;
            }
            case "RESTAURANT_TEAM" -> {
                log.debug("🏢👥 Returning RestaurantTeamOrchestrator for type RESTAURANT_TEAM");
                yield restaurantTeamOrchestrator;
            }
            case "CUSTOMER" -> {
                log.debug("👤 Returning CustomerOrchestrator for type CUSTOMER");
                yield customerOrchestrator;
            }
            case "AGENCY" -> {
                log.debug("🏛️ Returning AgencyUserOrchestrator for type AGENCY");
                yield agencyOrchestrator;
            }
            case "ADMIN" -> {
                log.debug("👨‍💼 Returning AdminOrchestrator for type ADMIN");
                yield adminOrchestrator;
            }
            default -> throw new IllegalArgumentException("Unknown user type: " + userType);
        };

        return (NotificationOrchestrator<T>) orchestrator;
    }

    /**
     * Gets the orchestrator for a specific user type (enum version).
     * 
     * @param userTypeEnum User type enum
     * @return NotificationOrchestrator for the user type
     */
    public <T extends ANotification> NotificationOrchestrator<T> getOrchestrator(UserType userTypeEnum) {
        return getOrchestrator(userTypeEnum.name());
    }

    /**
     * USER TYPE ENUM (for type-safe usage)
     * 
     * Usage:
     * factory.getOrchestrator(UserType.RESTAURANT)
     * factory.getOrchestrator(UserType.CUSTOMER)
     */
    public enum UserType {
        RESTAURANT,
        CUSTOMER,
        AGENCY,
        ADMIN
    }
}
