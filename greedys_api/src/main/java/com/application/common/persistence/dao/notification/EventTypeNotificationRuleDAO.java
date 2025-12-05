package com.application.common.persistence.dao.notification;

import com.application.common.persistence.model.notification.preferences.EventTypeNotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per EventTypeNotificationRule (Livello 1)
 * 
 * Query per caricare regole definite dall'admin per ogni EventType.
 */
@Repository
public interface EventTypeNotificationRuleDAO extends JpaRepository<EventTypeNotificationRule, Long> {

    /**
     * Trova regola per eventType esatto
     */
    Optional<EventTypeNotificationRule> findByEventType(String eventType);

    /**
     * Trova tutte le regole che matchano un eventType (esatto o wildcard)
     */
    @Query("""
        SELECT r FROM EventTypeNotificationRule r 
        WHERE r.eventType = :eventType 
        OR (:eventType LIKE CONCAT(REPLACE(r.eventType, '*', ''), '%') AND r.eventType LIKE '%*')
        """)
    List<EventTypeNotificationRule> findRulesForEventType(@Param("eventType") String eventType);

    /**
     * Trova tutti gli eventType che l'utente NON può disabilitare
     */
    List<EventTypeNotificationRule> findByUserCanDisableFalse();

    /**
     * Verifica se l'utente può disabilitare un eventType
     * Restituisce true se NON esiste regola O se userCanDisable=true
     */
    @Query("""
        SELECT CASE 
            WHEN COUNT(r) = 0 THEN true 
            ELSE MAX(CASE WHEN r.userCanDisable = true THEN 1 ELSE 0 END) = 1
        END
        FROM EventTypeNotificationRule r 
        WHERE r.eventType = :eventType 
        OR (:eventType LIKE CONCAT(REPLACE(r.eventType, '*', ''), '%') AND r.eventType LIKE '%*')
        """)
    boolean canUserDisable(@Param("eventType") String eventType);

    /**
     * Trova regole con canali mandatory specificati
     */
    @Query("SELECT r FROM EventTypeNotificationRule r WHERE r.mandatoryChannels IS NOT NULL AND r.mandatoryChannels != '[]'")
    List<EventTypeNotificationRule> findWithMandatoryChannels();
}
