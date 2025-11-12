package com.application.common.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.notification.ANotification;

/**
 * Repository base per ANotification (base per AdminNotification, RestaurantNotification, CustomerNotification, AgencyNotification)
 * 
 * ⭐ POLYMORPHIC QUERY:
 * - Usa `@Query("SELECT a FROM ANotification a ...")` per query generic
 * - Per subclass-specific: Usa AdminNotificationRepository, RestaurantNotificationRepository, etc.
 * 
 * @author Greedy's System
 * @since 2025-01-20 (Notification Base)
 */
@Repository
public interface ANotificationRepository<T extends ANotification> extends JpaRepository<T, Long> {

    /**
     * Trova tutte le notifiche di un user specifico.
     * 
     * @param userId L'ID dell'utente
     * @return Lista di notifiche per quel user
     */
    @Query("SELECT a FROM #{#entityName} a WHERE a.userId = :userId ORDER BY a.creationTime DESC")
    List<T> findByUserId(@Param("userId") Long userId);

    /**
     * Trova tutte le notifiche NON LETTE di un user.
     * 
     * @param userId L'ID dell'utente
     * @return Lista di notifiche non lette
     */
    @Query("SELECT a FROM #{#entityName} a WHERE a.userId = :userId AND a.read = false ORDER BY a.creationTime DESC")
    List<T> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Conta le notifiche NON LETTE di un user.
     * Utile per badge count in UI.
     * 
     * @param userId L'ID dell'utente
     * @return Numero di notifiche non lette
     */
    @Query("SELECT COUNT(a) FROM #{#entityName} a WHERE a.userId = :userId AND a.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Trova una notifica specifica per ID.
     * 
     * @param id L'ID della notifica
     * @return Optional della notifica
     */
    Optional<T> findById(Long id);

    /**
     * Marca una notifica come letta.
     * 
     * @param notificationId L'ID della notifica
     * @param userId L'ID dell'utente che ha letto
     * @param readAt Timestamp della lettura
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE #{#entityName} a SET a.read = true, a.readAt = :readAt WHERE a.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readAt") Instant readAt);

    /**
     * Marca tutte le notifiche di un user come lette.
     * 
     * @param userId L'ID dell'utente
     * @param readAt Timestamp della lettura
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE #{#entityName} a SET a.read = true, a.readAt = :readAt WHERE a.userId = :userId AND a.read = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    /**
     * Se sharedRead=true, marca la notifica come letta per TUTTI i recipient (eccetto chi ha già letto).
     * Usato per il First-To-Act pattern.
     * 
     * @param notificationId L'ID della notifica
     * @param readByUserId L'ID dell'utente che ha letto per primo
     * @param readAt Timestamp della lettura
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE #{#entityName} a SET a.read = true, a.readByUserId = :readByUserId, a.readAt = :readAt " +
           "WHERE a.id = :notificationId AND a.sharedRead = true AND a.read = false")
    void markAsReadShared(@Param("notificationId") Long notificationId, @Param("readByUserId") Long readByUserId, @Param("readAt") Instant readAt);

    /**
     * Pulisce le notifiche più vecchie di X giorni.
     * Da eseguire periodicamente per evitare accumuli di vecchie notifiche.
     * 
     * @param olderThanInstant Data limite
     * @return Numero di righe eliminate
     */
    @Modifying
    @Query("DELETE FROM #{#entityName} a WHERE a.creationTime < :olderThan")
    int deleteOlderThan(@Param("olderThan") Instant olderThanInstant);
}
