package com.application.common.persistence.dao.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.event.EventRSVP;
import com.application.common.persistence.model.event.RSVPStatus;

/**
 * ⭐ EVENT RSVP DAO
 * 
 * Repository per la gestione delle risposte (RSVP) agli eventi.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface EventRSVPDAO extends JpaRepository<EventRSVP, Long> {

    /**
     * Trova RSVP di un utente per un evento
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.userId = :userId")
    Optional<EventRSVP> findByEventIdAndUserId(
        @Param("eventId") Long eventId, 
        @Param("userId") Long userId
    );

    /**
     * Trova tutti i RSVP per un evento
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "ORDER BY r.createdAt ASC")
    Page<EventRSVP> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

    /**
     * Trova RSVP per status
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.status = :status " +
           "ORDER BY r.createdAt ASC")
    List<EventRSVP> findByEventIdAndStatus(
        @Param("eventId") Long eventId, 
        @Param("status") RSVPStatus status
    );

    /**
     * Conta partecipanti confermati
     */
    @Query("SELECT COALESCE(SUM(r.guestsCount), 0) FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.status IN ('GOING', 'CONFIRMED')")
    Integer countConfirmedAttendees(@Param("eventId") Long eventId);

    /**
     * Conta persone in waitlist
     */
    @Query("SELECT COUNT(r) FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.status = 'WAITLIST'")
    Long countWaitlist(@Param("eventId") Long eventId);

    /**
     * Trova RSVP di un utente (tutti i suoi eventi)
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.userId = :userId " +
           "ORDER BY r.event.eventDate ASC")
    Page<EventRSVP> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Trova RSVP futuri di un utente
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.userId = :userId " +
           "AND r.event.eventDate >= :today " +
           "AND r.status IN ('GOING', 'CONFIRMED', 'WAITLIST') " +
           "ORDER BY r.event.eventDate ASC")
    List<EventRSVP> findUpcomingByUserId(
        @Param("userId") Long userId, 
        @Param("today") java.time.LocalDate today
    );

    /**
     * Verifica se utente ha già risposto a un evento
     */
    @Query("SELECT COUNT(r) > 0 FROM EventRSVP r " +
           "WHERE r.event.id = :eventId AND r.userId = :userId")
    boolean existsByEventIdAndUserId(
        @Param("eventId") Long eventId, 
        @Param("userId") Long userId
    );

    /**
     * Lista d'attesa ordinata
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.status = 'WAITLIST' " +
           "ORDER BY r.waitlistPosition ASC")
    List<EventRSVP> findWaitlistByEventId(@Param("eventId") Long eventId);

    /**
     * Prossima posizione in waitlist
     */
    @Query("SELECT COALESCE(MAX(r.waitlistPosition), 0) + 1 FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.status = 'WAITLIST'")
    Integer getNextWaitlistPosition(@Param("eventId") Long eventId);

    /**
     * RSVP per check-in (arrivati)
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.checkedIn = true " +
           "ORDER BY r.checkedInAt DESC")
    List<EventRSVP> findCheckedIn(@Param("eventId") Long eventId);

    /**
     * RSVP che non hanno ricevuto reminder
     */
    @Query("SELECT r FROM EventRSVP r " +
           "WHERE r.event.id = :eventId " +
           "AND r.status IN ('GOING', 'CONFIRMED') " +
           "AND r.reminderSent = false")
    List<EventRSVP> findNeedingReminder(@Param("eventId") Long eventId);

    /**
     * Aggiorna status
     */
    @Modifying
    @Query("UPDATE EventRSVP r SET r.status = :status, r.updatedAt = :now WHERE r.id = :rsvpId")
    void updateStatus(@Param("rsvpId") Long rsvpId, @Param("status") RSVPStatus status, @Param("now") Instant now);

    /**
     * Segna come check-in
     */
    @Modifying
    @Query("UPDATE EventRSVP r " +
           "SET r.checkedIn = true, r.checkedInAt = :now, r.checkedInBy = :byUserId " +
           "WHERE r.id = :rsvpId")
    void checkIn(@Param("rsvpId") Long rsvpId, @Param("byUserId") Long byUserId, @Param("now") Instant now);

    /**
     * Segna reminder come inviato
     */
    @Modifying
    @Query("UPDATE EventRSVP r " +
           "SET r.reminderSent = true, r.reminderSentAt = :now " +
           "WHERE r.id = :rsvpId")
    void markReminderSent(@Param("rsvpId") Long rsvpId, @Param("now") Instant now);
}
