package com.application.common.persistence.dao.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.support.RequesterType;
import com.application.common.persistence.model.support.SupportTicket;
import com.application.common.persistence.model.support.TicketCategory;
import com.application.common.persistence.model.support.TicketPriority;
import com.application.common.persistence.model.support.TicketStatus;

/**
 * ⭐ SUPPORT TICKET DAO
 * 
 * Repository per la gestione dei ticket di supporto.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SupportTicketDAO extends JpaRepository<SupportTicket, Long> {

    /**
     * Trova ticket per numero (public ID)
     */
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    /**
     * Trova ticket di un utente (paginati)
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.requesterId = :requesterId " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByRequesterId(@Param("requesterId") Long requesterId, Pageable pageable);

    /**
     * Trova ticket di un utente per stato
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.requesterId = :requesterId " +
           "AND t.status = :status " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByRequesterIdAndStatus(
        @Param("requesterId") Long requesterId, 
        @Param("status") TicketStatus status,
        Pageable pageable
    );

    /**
     * Trova ticket aperti (per dashboard supporto)
     * Versione senza parametri - usa stati di default
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'WAITING_BOT', 'ESCALATED') " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<SupportTicket> findOpenTickets(Pageable pageable);

    /**
     * Trova ticket aperti (per dashboard supporto) con stati specifici
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.status IN :openStatuses " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<SupportTicket> findOpenTicketsWithStatuses(@Param("openStatuses") List<TicketStatus> openStatuses, Pageable pageable);

    /**
     * Trova ticket assegnati a un operatore
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.assignedToId = :operatorId " +
           "AND t.status NOT IN ('CLOSED', 'RESOLVED') " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<SupportTicket> findByAssignedToId(@Param("operatorId") Long operatorId, Pageable pageable);

    /**
     * Trova ticket assegnati a un operatore con stati specifici
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.assignedToId = :operatorId " +
           "AND t.status NOT IN :closedStatuses " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<SupportTicket> findByAssignedToIdWithStatuses(
        @Param("operatorId") Long operatorId, 
        @Param("closedStatuses") List<TicketStatus> closedStatuses,
        Pageable pageable
    );

    /**
     * Trova ticket non assegnati
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.assignedToId IS NULL " +
           "AND t.status IN :openStatuses " +
           "ORDER BY t.priority DESC, t.createdAt ASC")
    Page<SupportTicket> findUnassignedTickets(@Param("openStatuses") List<TicketStatus> openStatuses, Pageable pageable);

    /**
     * Trova ticket per categoria
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.category = :category " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByCategory(@Param("category") TicketCategory category, Pageable pageable);

    /**
     * Trova ticket per priorità (emergenze)
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.priority = :priority " +
           "AND t.status NOT IN :closedStatuses " +
           "ORDER BY t.createdAt ASC")
    List<SupportTicket> findByPriorityAndOpen(
        @Param("priority") TicketPriority priority, 
        @Param("closedStatuses") List<TicketStatus> closedStatuses
    );

    /**
     * Trova ticket per ristorante (segnalati su un locale)
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.restaurantId = :restaurantId " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByRestaurantId(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Trova ticket per prenotazione
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.reservationId = :reservationId " +
           "ORDER BY t.createdAt DESC")
    List<SupportTicket> findByReservationId(@Param("reservationId") Long reservationId);

    /**
     * Trova ticket per tipo richiedente
     */
    Page<SupportTicket> findByRequesterType(RequesterType requesterType, Pageable pageable);

    /**
     * Conta ticket aperti per utente
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t " +
           "WHERE t.requesterId = :requesterId " +
           "AND t.status NOT IN :closedStatuses")
    Long countOpenByRequesterId(
        @Param("requesterId") Long requesterId, 
        @Param("closedStatuses") List<TicketStatus> closedStatuses
    );

    /**
     * Conta ticket per stato (statistiche)
     */
    @Query("SELECT t.status, COUNT(t) FROM SupportTicket t " +
           "GROUP BY t.status")
    List<Object[]> countByStatus();

    /**
     * Conta ticket per categoria (statistiche)
     */
    @Query("SELECT t.category, COUNT(t) FROM SupportTicket t " +
           "WHERE t.createdAt >= :since " +
           "GROUP BY t.category")
    List<Object[]> countByCategorySince(@Param("since") java.time.Instant since);

    /**
     * Cerca ticket per subject/descrizione
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE LOWER(t.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Trova ticket in attesa BOT (per escalation automatica)
     */
    @Query("SELECT t FROM SupportTicket t " +
           "WHERE t.status = 'WAITING_BOT' " +
           "AND t.botHandled = true " +
           "AND t.updatedAt < :timeout " +
           "ORDER BY t.createdAt ASC")
    List<SupportTicket> findBotTimedOutTickets(@Param("timeout") java.time.Instant timeout);

    /**
     * Calcola tempo medio di risoluzione (statistiche)
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.resolvedAt)) FROM SupportTicket t " +
           "WHERE t.resolvedAt IS NOT NULL " +
           "AND t.createdAt >= :since")
    Double calculateAverageResolutionTimeHours(@Param("since") java.time.Instant since);
}
