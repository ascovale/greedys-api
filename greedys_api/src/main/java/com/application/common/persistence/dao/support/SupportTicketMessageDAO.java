package com.application.common.persistence.dao.support;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.support.SupportTicketMessage;

/**
 * ⭐ SUPPORT TICKET MESSAGE DAO
 * 
 * Repository per la gestione dei messaggi nei ticket di supporto.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SupportTicketMessageDAO extends JpaRepository<SupportTicketMessage, Long> {

    /**
     * Trova messaggi di un ticket (ordine cronologico)
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "ORDER BY m.createdAt ASC")
    List<SupportTicketMessage> findByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Trova messaggi di un ticket (paginati DESC per ultimi primi)
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "ORDER BY m.createdAt DESC")
    Page<SupportTicketMessage> findByTicketIdPaged(@Param("ticketId") Long ticketId, Pageable pageable);

    /**
     * Trova ultimo messaggio di un ticket
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "ORDER BY m.createdAt DESC " +
           "LIMIT 1")
    SupportTicketMessage findLastByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Trova messaggi del BOT per un ticket
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "AND m.isFromBot = true " +
           "ORDER BY m.createdAt ASC")
    List<SupportTicketMessage> findBotMessagesByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Trova messaggi staff per un ticket
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "AND m.isFromStaff = true " +
           "ORDER BY m.createdAt ASC")
    List<SupportTicketMessage> findStaffMessagesByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Conta messaggi di un ticket
     */
    Long countByTicketId(Long ticketId);

    /**
     * Conta messaggi BOT di un ticket
     */
    @Query("SELECT COUNT(m) FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "AND m.isFromBot = true")
    Long countBotMessagesByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Trova messaggi con allegati
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "AND m.attachmentUrl IS NOT NULL " +
           "ORDER BY m.createdAt DESC")
    List<SupportTicketMessage> findWithAttachments(@Param("ticketId") Long ticketId);

    /**
     * Trova messaggi interni (note staff)
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "AND m.isInternal = true " +
           "ORDER BY m.createdAt ASC")
    List<SupportTicketMessage> findInternalNotes(@Param("ticketId") Long ticketId);

    /**
     * Trova messaggi pubblici (visibili al cliente)
     */
    @Query("SELECT m FROM SupportTicketMessage m " +
           "WHERE m.ticket.id = :ticketId " +
           "AND m.isInternal = false " +
           "ORDER BY m.createdAt ASC")
    List<SupportTicketMessage> findPublicMessages(@Param("ticketId") Long ticketId);
}
