package com.application.common.persistence.dao.notification;

import com.application.common.persistence.model.notification.preferences.HubNotificationBlock;
import com.application.common.persistence.model.notification.preferences.HubNotificationBlock.HubType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per HubNotificationBlock (Livello 3)
 * 
 * Query per caricare blocchi a livello Hub (RUserHub / AgencyUserHub).
 */
@Repository
public interface HubNotificationBlockDAO extends JpaRepository<HubNotificationBlock, Long> {

    /**
     * Trova tutti i blocchi attivi per un hub
     */
    List<HubNotificationBlock> findByHubTypeAndHubIdAndActiveTrue(HubType hubType, Long hubId);

    /**
     * Trova blocco specifico per hub e eventType
     */
    Optional<HubNotificationBlock> findByHubTypeAndHubIdAndEventTypeAndActiveTrue(
        HubType hubType,
        Long hubId,
        String eventType
    );

    /**
     * Trova blocchi che matchano un eventType (esatto o wildcard) per un hub
     */
    @Query("""
        SELECT hnb FROM HubNotificationBlock hnb 
        WHERE hnb.hubType = :hubType 
        AND hnb.hubId = :hubId 
        AND hnb.active = true 
        AND (
            hnb.eventType IS NULL
            OR hnb.eventType = :eventType 
            OR hnb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(hnb.eventType, '*', ''), '%') AND hnb.eventType LIKE '%*')
        )
        """)
    List<HubNotificationBlock> findActiveBlocksForEventType(
        @Param("hubType") HubType hubType,
        @Param("hubId") Long hubId,
        @Param("eventType") String eventType
    );

    /**
     * Trova tutti i blocchi per un RUserHub
     */
    default List<HubNotificationBlock> findByRestaurantUserHubId(Long hubId) {
        return findByHubTypeAndHubIdAndActiveTrue(HubType.RESTAURANT_USER_HUB, hubId);
    }

    /**
     * Trova tutti i blocchi per un AgencyUserHub
     */
    default List<HubNotificationBlock> findByAgencyUserHubId(Long hubId) {
        return findByHubTypeAndHubIdAndActiveTrue(HubType.AGENCY_USER_HUB, hubId);
    }

    /**
     * Verifica se un canale è bloccato per un hub e eventType
     */
    @Query("""
        SELECT CASE WHEN COUNT(hnb) > 0 THEN true ELSE false END
        FROM HubNotificationBlock hnb 
        WHERE hnb.hubType = :hubType 
        AND hnb.hubId = :hubId 
        AND hnb.active = true 
        AND (
            hnb.eventType IS NULL
            OR hnb.eventType = :eventType 
            OR hnb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(hnb.eventType, '*', ''), '%') AND hnb.eventType LIKE '%*')
        )
        AND (
            hnb.blockedChannels IS NULL 
            OR hnb.blockedChannels LIKE CONCAT('%', :channel, '%')
        )
        """)
    boolean isChannelBlocked(
        @Param("hubType") HubType hubType,
        @Param("hubId") Long hubId,
        @Param("eventType") String eventType,
        @Param("channel") String channel
    );
}
