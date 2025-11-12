package com.application.common.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.AdminNotification;

/**
 * Repository per AdminNotification
 * 
 * Specializzato per le notifiche degli admin.
 * 
 * @author Greedy's System
 * @since 2025-01-20
 */
@Repository
public interface AdminNotificationRepository extends ANotificationRepository<AdminNotification> {

    /**
     * Trova tutte le notifiche di un admin specifico per un ristorante.
     * 
     * @param restaurantId L'ID del ristorante
     * @param userId L'ID dell'admin
     * @return Lista di AdminNotification per quel admin in quel ristorante
     */
    @Query("SELECT a FROM AdminNotification a WHERE a.restaurantId = :restaurantId AND a.userId = :userId ORDER BY a.creationTime DESC")
    List<AdminNotification> findByRestaurantAndUser(@Param("restaurantId") Long restaurantId, @Param("userId") Long userId);

    /**
     * Conta le notifiche NON LETTE di un admin per un ristorante.
     * 
     * @param restaurantId L'ID del ristorante
     * @param userId L'ID dell'admin
     * @return Numero di notifiche non lette
     */
    @Query("SELECT COUNT(a) FROM AdminNotification a WHERE a.restaurantId = :restaurantId AND a.userId = :userId AND a.read = false")
    long countUnreadByRestaurantAndUser(@Param("restaurantId") Long restaurantId, @Param("userId") Long userId);
}
