package com.application.common.persistence.dao.notification;

import com.application.common.persistence.model.notification.preferences.UserNotificationBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per UserNotificationBlock (Livello 4)
 * 
 * Query per caricare blocchi a livello singolo utente.
 */
@Repository
public interface UserNotificationBlockDAO extends JpaRepository<UserNotificationBlock, Long> {

    /**
     * Trova tutti i blocchi attivi per un utente
     */
    List<UserNotificationBlock> findByUserIdAndActiveTrue(Long userId);

    /**
     * Trova blocco specifico per utente e eventType
     */
    Optional<UserNotificationBlock> findByUserIdAndEventTypeAndActiveTrue(Long userId, String eventType);

    /**
     * Trova blocchi che matchano un eventType (esatto o wildcard) per un utente
     */
    @Query("""
        SELECT unb FROM UserNotificationBlock unb 
        WHERE unb.userId = :userId 
        AND unb.active = true 
        AND (
            unb.eventType IS NULL
            OR unb.eventType = :eventType 
            OR unb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(unb.eventType, '*', ''), '%') AND unb.eventType LIKE '%*')
        )
        """)
    List<UserNotificationBlock> findActiveBlocksForEventType(
        @Param("userId") Long userId,
        @Param("eventType") String eventType
    );

    /**
     * Verifica se un canale è bloccato per un utente e eventType
     */
    @Query("""
        SELECT CASE WHEN COUNT(unb) > 0 THEN true ELSE false END
        FROM UserNotificationBlock unb 
        WHERE unb.userId = :userId 
        AND unb.active = true 
        AND (
            unb.eventType IS NULL
            OR unb.eventType = :eventType 
            OR unb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(unb.eventType, '*', ''), '%') AND unb.eventType LIKE '%*')
        )
        AND (
            unb.blockedChannels IS NULL 
            OR unb.blockedChannels LIKE CONCAT('%', :channel, '%')
        )
        """)
    boolean isChannelBlocked(
        @Param("userId") Long userId,
        @Param("eventType") String eventType,
        @Param("channel") String channel
    );

    /**
     * Verifica se l'utente ha bloccato completamente un eventType (tutti i canali)
     */
    @Query("""
        SELECT CASE WHEN COUNT(unb) > 0 THEN true ELSE false END
        FROM UserNotificationBlock unb 
        WHERE unb.userId = :userId 
        AND unb.active = true 
        AND unb.blockedChannels IS NULL
        AND (
            unb.eventType IS NULL
            OR unb.eventType = :eventType 
            OR unb.eventType = '*'
            OR (:eventType LIKE CONCAT(REPLACE(unb.eventType, '*', ''), '%') AND unb.eventType LIKE '%*')
        )
        """)
    boolean isEventTypeFullyBlocked(
        @Param("userId") Long userId,
        @Param("eventType") String eventType
    );

    /**
     * Conta blocchi per utente (per UI - mostrare quante notifiche ha bloccato)
     */
    long countByUserIdAndActiveTrue(Long userId);

    /**
     * Elimina tutti i blocchi di un utente (reset preferenze)
     */
    void deleteByUserId(Long userId);
}
