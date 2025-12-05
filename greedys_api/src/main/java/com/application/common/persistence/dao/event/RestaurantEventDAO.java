package com.application.common.persistence.dao.event;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.event.EventStatus;
import com.application.common.persistence.model.event.RestaurantEvent;
import com.application.common.persistence.model.event.RestaurantEventType;

/**
 * ⭐ RESTAURANT EVENT DAO
 * 
 * Repository per la gestione degli eventi ristorante.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface RestaurantEventDAO extends JpaRepository<RestaurantEvent, Long> {

    /**
     * Trova eventi di un ristorante
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.restaurantId = :restaurantId " +
           "AND e.isDeleted = false " +
           "ORDER BY e.eventDate ASC, e.startTime ASC")
    Page<RestaurantEvent> findByRestaurantId(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Trova eventi futuri di un ristorante
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.restaurantId = :restaurantId " +
           "AND e.eventDate >= :today " +
           "AND e.status IN ('PUBLISHED', 'SOLD_OUT') " +
           "AND e.isDeleted = false " +
           "ORDER BY e.eventDate ASC, e.startTime ASC")
    List<RestaurantEvent> findUpcomingByRestaurantId(
        @Param("restaurantId") Long restaurantId, 
        @Param("today") LocalDate today
    );

    /**
     * Trova eventi per data
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.eventDate = :date " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false " +
           "ORDER BY e.startTime ASC")
    List<RestaurantEvent> findByEventDate(@Param("date") LocalDate date);

    /**
     * Trova eventi in un range di date
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.eventDate BETWEEN :startDate AND :endDate " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false " +
           "ORDER BY e.eventDate ASC, e.startTime ASC")
    Page<RestaurantEvent> findByDateRange(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * Trova eventi per tipo
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.eventType = :eventType " +
           "AND e.eventDate >= :today " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false " +
           "ORDER BY e.eventDate ASC")
    Page<RestaurantEvent> findByEventType(
        @Param("eventType") RestaurantEventType eventType, 
        @Param("today") LocalDate today,
        Pageable pageable
    );

    /**
     * Trova eventi featured
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.isFeatured = true " +
           "AND e.eventDate >= :today " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false " +
           "ORDER BY e.eventDate ASC")
    List<RestaurantEvent> findFeatured(@Param("today") LocalDate today);

    /**
     * Trova eventi con posti disponibili
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.eventDate >= :today " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false " +
           "AND (e.maxCapacity IS NULL OR e.currentAttendees < e.maxCapacity) " +
           "ORDER BY e.eventDate ASC")
    Page<RestaurantEvent> findAvailable(@Param("today") LocalDate today, Pageable pageable);

    /**
     * Conta eventi futuri per ristorante
     */
    @Query("SELECT COUNT(e) FROM RestaurantEvent e " +
           "WHERE e.restaurantId = :restaurantId " +
           "AND e.eventDate >= :today " +
           "AND e.isDeleted = false")
    Long countUpcomingByRestaurantId(
        @Param("restaurantId") Long restaurantId, 
        @Param("today") LocalDate today
    );

    /**
     * Cerca eventi per titolo/descrizione
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.isDeleted = false " +
           "AND e.status = 'PUBLISHED' " +
           "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY e.eventDate ASC")
    Page<RestaurantEvent> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Aggiorna status
     */
    @Modifying
    @Query("UPDATE RestaurantEvent e SET e.status = :status WHERE e.id = :eventId")
    void updateStatus(@Param("eventId") Long eventId, @Param("status") EventStatus status);

    /**
     * Incrementa views
     */
    @Modifying
    @Query("UPDATE RestaurantEvent e SET e.viewsCount = e.viewsCount + 1 WHERE e.id = :eventId")
    void incrementViews(@Param("eventId") Long eventId);

    /**
     * Incrementa attendees
     */
    @Modifying
    @Query("UPDATE RestaurantEvent e SET e.currentAttendees = e.currentAttendees + :count WHERE e.id = :eventId")
    void incrementAttendees(@Param("eventId") Long eventId, @Param("count") int count);

    /**
     * Decrementa attendees
     */
    @Modifying
    @Query("UPDATE RestaurantEvent e " +
           "SET e.currentAttendees = GREATEST(0, e.currentAttendees - :count) " +
           "WHERE e.id = :eventId")
    void decrementAttendees(@Param("eventId") Long eventId, @Param("count") int count);

    /**
     * Eventi che stanno per iniziare (per reminder)
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.eventDate = :date " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false")
    List<RestaurantEvent> findEventsOnDate(@Param("date") LocalDate date);

    /**
     * Eventi passati non completati (per cleanup)
     */
    @Query("SELECT e FROM RestaurantEvent e " +
           "WHERE e.eventDate < :today " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.isDeleted = false")
    List<RestaurantEvent> findPastNotCompleted(@Param("today") LocalDate today);
}
