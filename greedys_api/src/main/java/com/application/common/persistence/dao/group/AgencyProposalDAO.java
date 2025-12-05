package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.AgencyProposal;
import com.application.common.persistence.model.group.enums.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO per AgencyProposal - Proposte personalizzate per agency
 */
@Repository
public interface AgencyProposalDAO extends JpaRepository<AgencyProposal, Long> {

    // ==================== BY CODE ====================

    Optional<AgencyProposal> findByProposalCode(String proposalCode);

    // ==================== BY RESTAURANT ====================

    Page<AgencyProposal> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT p FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId " +
           "AND p.status = :status ORDER BY p.updatedAt DESC")
    Page<AgencyProposal> findByRestaurantAndStatus(
        @Param("restaurantId") Long restaurantId, 
        @Param("status") ProposalStatus status, 
        Pageable pageable);

    @Query("SELECT p FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId " +
           "AND p.status NOT IN ('REJECTED', 'EXPIRED', 'CANCELLED') " +
           "ORDER BY p.updatedAt DESC")
    List<AgencyProposal> findActiveByRestaurant(@Param("restaurantId") Long restaurantId);

    // ==================== BY AGENCY ====================

    Page<AgencyProposal> findByAgencyId(Long agencyId, Pageable pageable);

    @Query("SELECT p FROM AgencyProposal p WHERE p.agency.id = :agencyId " +
           "AND p.status = :status ORDER BY p.updatedAt DESC")
    Page<AgencyProposal> findByAgencyAndStatus(
        @Param("agencyId") Long agencyId, 
        @Param("status") ProposalStatus status, 
        Pageable pageable);

    @Query("SELECT p FROM AgencyProposal p WHERE p.agency.id = :agencyId " +
           "AND p.status NOT IN ('REJECTED', 'EXPIRED', 'CANCELLED') " +
           "ORDER BY p.updatedAt DESC")
    List<AgencyProposal> findActiveByAgency(@Param("agencyId") Long agencyId);

    // ==================== BY RESTAURANT + AGENCY ====================

    @Query("SELECT p FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId " +
           "AND p.agency.id = :agencyId ORDER BY p.createdAt DESC")
    List<AgencyProposal> findByRestaurantAndAgency(
        @Param("restaurantId") Long restaurantId, 
        @Param("agencyId") Long agencyId);

    @Query("SELECT p FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId " +
           "AND p.agency.id = :agencyId " +
           "AND p.status IN ('ACTIVE', 'ACCEPTED', 'SENT') " +
           "ORDER BY p.updatedAt DESC")
    List<AgencyProposal> findActiveByRestaurantAndAgency(
        @Param("restaurantId") Long restaurantId, 
        @Param("agencyId") Long agencyId);

    // ==================== EXCLUSIVE ====================

    @Query("SELECT p FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId " +
           "AND p.agency.id = :agencyId " +
           "AND p.isExclusive = true " +
           "AND p.status IN ('ACTIVE', 'ACCEPTED', 'SENT')")
    List<AgencyProposal> findExclusiveProposals(
        @Param("restaurantId") Long restaurantId, 
        @Param("agencyId") Long agencyId);

    // ==================== VALIDITY ====================

    @Query("SELECT p FROM AgencyProposal p WHERE p.status IN ('ACTIVE', 'SENT') " +
           "AND p.validUntil IS NOT NULL AND p.validUntil < :today")
    List<AgencyProposal> findExpiredProposals(@Param("today") LocalDate today);

    @Query("SELECT p FROM AgencyProposal p WHERE p.agency.id = :agencyId " +
           "AND p.status IN ('ACTIVE', 'ACCEPTED') " +
           "AND (p.validFrom IS NULL OR p.validFrom <= :today) " +
           "AND (p.validUntil IS NULL OR p.validUntil >= :today)")
    List<AgencyProposal> findValidByAgency(
        @Param("agencyId") Long agencyId, 
        @Param("today") LocalDate today);

    // ==================== PENDING RESPONSE ====================

    @Query("SELECT p FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId " +
           "AND p.status IN ('PENDING_RESTAURANT', 'COUNTER_PROPOSAL') " +
           "AND p.lastModifiedByType = 'AGENCY'")
    List<AgencyProposal> findPendingRestaurantResponse(@Param("restaurantId") Long restaurantId);

    @Query("SELECT p FROM AgencyProposal p WHERE p.agency.id = :agencyId " +
           "AND p.status IN ('SENT', 'PENDING_AGENCY', 'COUNTER_PROPOSAL') " +
           "AND p.lastModifiedByType = 'RESTAURANT'")
    List<AgencyProposal> findPendingAgencyResponse(@Param("agencyId") Long agencyId);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(p) FROM AgencyProposal p WHERE p.restaurant.id = :restaurantId AND p.status = :status")
    Long countByRestaurantAndStatus(
        @Param("restaurantId") Long restaurantId, 
        @Param("status") ProposalStatus status);

    @Query("SELECT COUNT(p) FROM AgencyProposal p WHERE p.agency.id = :agencyId AND p.status = 'ACCEPTED'")
    Long countAcceptedByAgency(@Param("agencyId") Long agencyId);
}
