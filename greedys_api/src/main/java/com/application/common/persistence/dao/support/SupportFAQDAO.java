package com.application.common.persistence.dao.support;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.support.RequesterType;
import com.application.common.persistence.model.support.SupportFAQ;
import com.application.common.persistence.model.support.TicketCategory;

/**
 * ⭐ SUPPORT FAQ DAO
 * 
 * Repository per la gestione delle FAQ per il BOT di supporto.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Repository
public interface SupportFAQDAO extends JpaRepository<SupportFAQ, Long> {

    /**
     * Trova FAQ attive per categoria
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.category = :category " +
           "AND f.isActive = true " +
           "ORDER BY f.displayOrder ASC")
    List<SupportFAQ> findActiveByCategory(@Param("category") TicketCategory category);

    /**
     * Trova FAQ per tipo richiedente
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.targetUserType = :requesterType " +
           "AND f.isActive = true " +
           "ORDER BY f.displayOrder ASC")
    List<SupportFAQ> findActiveByRequesterType(@Param("requesterType") RequesterType requesterType);

    /**
     * Trova FAQ per categoria e tipo richiedente
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.category = :category " +
           "AND f.targetUserType = :requesterType " +
           "AND f.isActive = true " +
           "ORDER BY f.displayOrder ASC")
    List<SupportFAQ> findActiveByCategoryAndRequesterType(
        @Param("category") TicketCategory category, 
        @Param("requesterType") RequesterType requesterType
    );

    /**
     * Cerca FAQ per keywords (fulltext search)
     */
    @Query("SELECT DISTINCT f FROM SupportFAQ f " +
           "JOIN f.keywords k " +
           "WHERE f.isActive = true " +
           "AND (LOWER(k) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(f.question) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<SupportFAQ> searchByKeywords(@Param("searchTerm") String searchTerm);

    /**
     * Cerca FAQ per keywords e tipo richiedente
     */
    @Query("SELECT DISTINCT f FROM SupportFAQ f " +
           "JOIN f.keywords k " +
           "WHERE f.isActive = true " +
           "AND f.targetUserType = :requesterType " +
           "AND (LOWER(k) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(f.question) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<SupportFAQ> searchByKeywordsAndRequesterType(
        @Param("searchTerm") String searchTerm, 
        @Param("requesterType") RequesterType requesterType
    );

    /**
     * Trova FAQ più usate (per dashboard)
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.isActive = true " +
           "ORDER BY f.usageCount DESC")
    Page<SupportFAQ> findMostUsed(Pageable pageable);

    /**
     * Trova FAQ più utili (per miglioramento)
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.isActive = true " +
           "AND f.usageCount > 0 " +
           "ORDER BY (f.helpfulCount * 1.0 / f.usageCount) DESC")
    Page<SupportFAQ> findMostHelpful(Pageable pageable);

    /**
     * Trova tutte le FAQ attive (per admin)
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.isActive = true " +
           "ORDER BY f.category ASC, f.displayOrder ASC")
    List<SupportFAQ> findAllActive();

    /**
     * Trova tutte le FAQ (incluse disattivate, per admin)
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "ORDER BY f.category ASC, f.displayOrder ASC")
    Page<SupportFAQ> findAllForAdmin(Pageable pageable);

    /**
     * Incrementa contatore di utilizzo
     */
    @Modifying
    @Query("UPDATE SupportFAQ f " +
           "SET f.usageCount = f.usageCount + 1 " +
           "WHERE f.id = :faqId")
    void incrementUsageCount(@Param("faqId") Long faqId);

    /**
     * Incrementa contatore helpful
     */
    @Modifying
    @Query("UPDATE SupportFAQ f " +
           "SET f.helpfulCount = f.helpfulCount + 1 " +
           "WHERE f.id = :faqId")
    void incrementHelpfulCount(@Param("faqId") Long faqId);

    /**
     * Trova FAQ con basso helpfulness (per revisione)
     */
    @Query("SELECT f FROM SupportFAQ f " +
           "WHERE f.isActive = true " +
           "AND f.usageCount >= :minUsage " +
           "AND (f.helpfulCount * 1.0 / f.usageCount) < :threshold " +
           "ORDER BY (f.helpfulCount * 1.0 / f.usageCount) ASC")
    List<SupportFAQ> findLowPerformingFAQs(
        @Param("minUsage") int minUsage, 
        @Param("threshold") double threshold
    );
}
