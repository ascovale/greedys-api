package com.application.common.persistence.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.NotificationPreferences;

/**
 * DAO per la gestione delle preferenze di notificazione dell'utente
 * 
 * Traccia quali canali sono abilitati per ogni utente
 * (EMAIL, SMS, PUSH, WEBSOCKET, SLACK) e le preferenze granulari per tipo di evento
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Repository
public interface NotificationPreferencesDAO extends JpaRepository<NotificationPreferences, Long> {
    
    /**
     * Trova le preferenze di notificazione per un utente specifico
     * 
     * @param userId ID dell'utente
     * @return Optional contenente le preferenze se trovate
     */
    Optional<NotificationPreferences> findByUserId(Long userId);
    
    /**
     * Trova le preferenze per un utente e tipo di utente specifico
     * 
     * @param userId ID dell'utente
     * @param userType Tipo di utente (CUSTOMER, RESTAURANT_USER, ADMIN_USER, AGENCY_USER)
     * @return Optional contenente le preferenze se trovate
     */
    Optional<NotificationPreferences> findByUserIdAndUserType(Long userId, String userType);
    
    /**
     * Verifica se un utente ha la notifica via email abilitata
     * 
     * @param userId ID dell'utente
     * @return true se email è abilitata
     */
    boolean existsByUserIdAndEmailEnabledTrue(Long userId);
    
    /**
     * Verifica se un utente ha la notifica via push abilitata
     * 
     * @param userId ID dell'utente
     * @return true se push è abilitato
     */
    boolean existsByUserIdAndPushEnabledTrue(Long userId);
    
    /**
     * Elimina le preferenze di un utente
     * 
     * @param userId ID dell'utente
     */
    void deleteByUserId(Long userId);
}
