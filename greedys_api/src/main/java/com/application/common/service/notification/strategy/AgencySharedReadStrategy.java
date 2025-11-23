package com.application.common.service.notification.strategy;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.agency.persistence.dao.AgencyUserNotificationDAO;

/**
 * Strategy for agency-scoped shared read operations
 * 
 * Mirrors RestaurantSharedReadStrategy but for Agency entity type
 * 
 * Supports scopes:
 * - RESTAURANT (reused enum, but means AGENCY here)
 * - RESTAURANT_HUB (reused enum, but means AGENCY_HUB here)
 * - RESTAURANT_HUB_ALL (reused enum, but means AGENCY_HUB_ALL here)
 * 
 * Future: Consider creating separate Agency-specific enum if needed
 * 
 * @author System
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgencySharedReadStrategy implements SharedReadStrategy {
    
    private final AgencyUserNotificationDAO agencyUserNotificationDAO;
    
    private static final Set<SharedReadScope> SUPPORTED_SCOPES = 
        EnumSet.of(
            SharedReadScope.RESTAURANT,  // Maps to AGENCY
            SharedReadScope.RESTAURANT_HUB,  // Maps to AGENCY_HUB
            SharedReadScope.RESTAURANT_HUB_ALL  // Maps to AGENCY_HUB_ALL
        );
    
    // ========== MAIN INTERFACE METHOD ==========
    
    @Override
    @Transactional
    public void markAsRead(SharedReadParams params) {
        
        params.validateAgency();
        
        String scopeStr = params.getScope();
        if (scopeStr == null || scopeStr.isEmpty()) {
            scopeStr = SharedReadScope.NONE.name();
        }
        
        try {
            SharedReadScope scope = SharedReadScope.valueOf(scopeStr);
            
            if (!SUPPORTED_SCOPES.contains(scope)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Scope %s not supported by AgencySharedReadStrategy. Supported: %s",
                        scope,
                        SUPPORTED_SCOPES
                    )
                );
            }
            
            log.debug(
                "Agency marking notification {} as read with scope {}",
                params.getNotificationId(),
                scope
            );
            
            switch (scope) {
                case RESTAURANT:  // Actually AGENCY
                    markAsReadAgency(params);
                    break;
                    
                case RESTAURANT_HUB:  // Actually AGENCY_HUB
                    markAsReadAgencyHub(params);
                    break;
                    
                case RESTAURANT_HUB_ALL:  // Actually AGENCY_HUB_ALL
                    markAsReadAgencyHubAll(params);
                    break;
                    
                default:
                    log.warn("Unexpected scope in switch: {}", scope);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid scope: {}", scopeStr, e);
            throw e;
        }
    }
    
    // ========== SCOPE-SPECIFIC HANDLERS ==========
    
    /**
     * AGENCY scope: Mark all notifications for agency as read
     * 
     * Use case: Agency-wide announcement
     *   - AgencyUser#1 (agency#3) reads
     *   - All AgencyUsers in agency#3 see it as read
     */
    private void markAsReadAgency(SharedReadParams params) {
        
        if (params.getAgencyId() == null) {
            throw new IllegalArgumentException(
                "agencyId required for AGENCY scope"
            );
        }
        
        int updated = agencyUserNotificationDAO.markAsReadAgency(
            params.getAgencyId(),
            params.getReadByUserId(),
            params.getReadAt()
        );
        
        log.info(
            "Marked {} agency notifications as read | "
            + "scope=AGENCY, agency_id={}, read_by_user={}",
            updated,
            params.getAgencyId(),
            params.getReadByUserId()
        );
    }
    
    /**
     * AGENCY_HUB scope: Mark all notifications across hub as read
     * 
     * Use case: Hub manager sends to all staff across multiple agencies
     *   - Hub#20 manages agency#1, agency#2
     *   - Hub manager reads
     *   - ALL staff in hub#20 see it as read
     */
    private void markAsReadAgencyHub(SharedReadParams params) {
        
        if (params.getAgencyUserHubId() == null) {
            throw new IllegalArgumentException(
                "agencyUserHubId required for AGENCY_HUB scope"
            );
        }
        
        int updated = agencyUserNotificationDAO.markAsReadAgencyHub(
            params.getAgencyUserHubId(),
            params.getReadByUserId(),
            params.getReadAt()
        );
        
        log.info(
            "Marked {} agency notifications as read | "
            + "scope=AGENCY_HUB, hub_id={}, read_by_user={}",
            updated,
            params.getAgencyUserHubId(),
            params.getReadByUserId()
        );
    }
    
    /**
     * AGENCY_HUB_ALL scope: Admin broadcast - mark ALL as read immediately
     */
    private void markAsReadAgencyHubAll(SharedReadParams params) {
        
        if (params.getAgencyUserHubId() == null) {
            throw new IllegalArgumentException(
                "agencyUserHubId required for AGENCY_HUB_ALL scope"
            );
        }
        
        int updated = agencyUserNotificationDAO.markAsReadAgencyHubAll(
            params.getAgencyUserHubId(),
            params.getReadByUserId(),
            params.getReadAt()
        );
        
        log.info(
            "Marked {} agency notifications as read (BROADCAST) | "
            + "scope=AGENCY_HUB_ALL, hub_id={}, read_by_user={}",
            updated,
            params.getAgencyUserHubId(),
            params.getReadByUserId()
        );
    }
    
    // ========== BATCH OPERATIONS ==========
    
    @Override
    @Transactional
    public void markMultipleAsRead(List<SharedReadParams> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            log.debug("No params to process");
            return;
        }
        
        log.debug("Processing batch of {} shared read operations", paramsList.size());
        
        paramsList.forEach(this::markAsRead);
        
        log.info("Completed batch processing of {} notifications", paramsList.size());
    }
    
    // ========== METADATA ==========
    
    @Override
    public Set<SharedReadScope> getSupportedScopes() {
        return SUPPORTED_SCOPES;
    }
}
