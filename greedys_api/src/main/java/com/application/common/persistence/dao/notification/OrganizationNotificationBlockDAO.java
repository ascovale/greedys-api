package com.application.common.persistence.dao.notification;

import com.application.common.persistence.model.notification.preferences.OrganizationNotificationBlock;
import com.application.common.persistence.model.notification.preferences.OrganizationNotificationBlock.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per OrganizationNotificationBlock (Livello 2)
 * 
 * Query per caricare blocchi a livello Restaurant/Agency.
 */
@Repository
public interface OrganizationNotificationBlockDAO extends JpaRepository<OrganizationNotificationBlock, Long> {

    /**
     * Trova tutti i blocchi attivi per un'organizzazione
     */
    List<OrganizationNotificationBlock> findByOrganizationTypeAndOrganizationIdAndActiveTrue(
        OrganizationType organizationType,
        Long organizationId
    );

    /**
     * Trova blocco specifico per organizzazione e eventType
     */
    Optional<OrganizationNotificationBlock> findByOrganizationTypeAndOrganizationIdAndEventTypeAndActiveTrue(
        OrganizationType organizationType,
        Long organizationId,
        String eventType
    );

    /**
     * Trova blocchi che matchano un eventType (esatto o wildcard) per un'organizzazione
     */
    @Query("""
        SELECT onb FROM OrganizationNotificationBlock onb 
        WHERE onb.organizationType = :orgType 
        AND onb.organizationId = :orgId 
        AND onb.active = true 
        AND (
            onb.eventType IS NULL
            OR onb.eventType = :eventType 
            OR onb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(onb.eventType, '*', ''), '%') AND onb.eventType LIKE '%*')
        )
        """)
    List<OrganizationNotificationBlock> findActiveBlocksForEventType(
        @Param("orgType") OrganizationType organizationType,
        @Param("orgId") Long organizationId,
        @Param("eventType") String eventType
    );

    /**
     * Trova tutti i blocchi per un Restaurant
     */
    default List<OrganizationNotificationBlock> findByRestaurantId(Long restaurantId) {
        return findByOrganizationTypeAndOrganizationIdAndActiveTrue(OrganizationType.RESTAURANT, restaurantId);
    }

    /**
     * Trova tutti i blocchi per un'Agency
     */
    default List<OrganizationNotificationBlock> findByAgencyId(Long agencyId) {
        return findByOrganizationTypeAndOrganizationIdAndActiveTrue(OrganizationType.AGENCY, agencyId);
    }

    /**
     * Verifica se un canale è bloccato per un'organizzazione e eventType
     */
    @Query("""
        SELECT CASE WHEN COUNT(onb) > 0 THEN true ELSE false END
        FROM OrganizationNotificationBlock onb 
        WHERE onb.organizationType = :orgType 
        AND onb.organizationId = :orgId 
        AND onb.active = true 
        AND (
            onb.eventType IS NULL
            OR onb.eventType = :eventType 
            OR onb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(onb.eventType, '*', ''), '%') AND onb.eventType LIKE '%*')
        )
        AND (
            onb.blockedChannels IS NULL 
            OR onb.blockedChannels LIKE CONCAT('%', :channel, '%')
        )
        """)
    boolean isChannelBlocked(
        @Param("orgType") OrganizationType organizationType,
        @Param("orgId") Long organizationId,
        @Param("eventType") String eventType,
        @Param("channel") String channel
    );
}
