package com.application.common.persistence.dao.notification;

import com.application.common.persistence.model.notification.preferences.GlobalNotificationBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * DAO per GlobalNotificationBlock (Livello 0)
 * 
 * Query ottimizzate per verificare blocchi globali.
 */
@Repository
public interface GlobalNotificationBlockDAO extends JpaRepository<GlobalNotificationBlock, Long> {

    /**
     * Trova blocco esatto per eventType
     */
    Optional<GlobalNotificationBlock> findByEventTypeAndActiveTrue(String eventType);

    /**
     * Trova tutti i blocchi attivi
     */
    List<GlobalNotificationBlock> findByActiveTrue();

    /**
     * Verifica se un eventType è bloccato (esatto o wildcard)
     * Cerca match esatto O pattern wildcard (es: "SOCIAL_*" matcha "SOCIAL_NEW_POST")
     */
    @Query("""
        SELECT gnb FROM GlobalNotificationBlock gnb 
        WHERE gnb.active = true 
        AND (
            gnb.eventType = :eventType 
            OR (:eventType LIKE CONCAT(REPLACE(gnb.eventType, '*', ''), '%') AND gnb.eventType LIKE '%*')
        )
        AND (gnb.blockStart IS NULL OR gnb.blockStart <= :now)
        AND (gnb.blockEnd IS NULL OR gnb.blockEnd >= :now)
        """)
    List<GlobalNotificationBlock> findActiveBlocksForEventType(
        @Param("eventType") String eventType,
        @Param("now") Instant now
    );

    /**
     * Verifica veloce se eventType è bloccato
     */
    @Query("""
        SELECT CASE WHEN COUNT(gnb) > 0 THEN true ELSE false END
        FROM GlobalNotificationBlock gnb 
        WHERE gnb.active = true 
        AND (
            gnb.eventType = :eventType 
            OR (:eventType LIKE CONCAT(REPLACE(gnb.eventType, '*', ''), '%') AND gnb.eventType LIKE '%*')
        )
        AND (gnb.blockStart IS NULL OR gnb.blockStart <= :now)
        AND (gnb.blockEnd IS NULL OR gnb.blockEnd >= :now)
        """)
    boolean isEventTypeBlocked(
        @Param("eventType") String eventType,
        @Param("now") Instant now
    );

    /**
     * Trova blocchi scaduti (per cleanup)
     */
    @Query("SELECT gnb FROM GlobalNotificationBlock gnb WHERE gnb.blockEnd < :now AND gnb.active = true")
    List<GlobalNotificationBlock> findExpiredBlocks(@Param("now") Instant now);
}
