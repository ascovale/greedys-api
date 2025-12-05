package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.RestaurantAgency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO per RestaurantAgency - Relazione B2B ristorante-agenzia
 */
@Repository
public interface RestaurantAgencyDAO extends JpaRepository<RestaurantAgency, Long> {

    // ==================== FIND RELATIONSHIP ====================

    Optional<RestaurantAgency> findByRestaurantIdAndAgencyId(Long restaurantId, Long agencyId);

    boolean existsByRestaurantIdAndAgencyId(Long restaurantId, Long agencyId);

    // ==================== BY RESTAURANT ====================

    Page<RestaurantAgency> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.restaurant.id = :restaurantId " +
           "AND ra.isActive = true AND ra.isApproved = true")
    List<RestaurantAgency> findActiveByRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.restaurant.id = :restaurantId " +
           "AND ra.isActive = true AND ra.isApproved = false")
    List<RestaurantAgency> findPendingByRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.restaurant.id = :restaurantId " +
           "AND ra.isTrusted = true")
    List<RestaurantAgency> findTrustedByRestaurant(@Param("restaurantId") Long restaurantId);

    // ==================== BY AGENCY ====================

    Page<RestaurantAgency> findByAgencyId(Long agencyId, Pageable pageable);

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.agency.id = :agencyId " +
           "AND ra.isActive = true AND ra.isApproved = true")
    List<RestaurantAgency> findActiveByAgency(@Param("agencyId") Long agencyId);

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.agency.id = :agencyId " +
           "AND ra.isActive = true AND ra.isApproved = false")
    List<RestaurantAgency> findPendingByAgency(@Param("agencyId") Long agencyId);

    // ==================== CAN BOOK ====================

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.restaurant.id = :restaurantId " +
           "AND ra.agency.id = :agencyId " +
           "AND ra.isActive = true AND ra.isApproved = true")
    Optional<RestaurantAgency> findActiveRelationship(
        @Param("restaurantId") Long restaurantId, 
        @Param("agencyId") Long agencyId);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(ra) FROM RestaurantAgency ra WHERE ra.restaurant.id = :restaurantId AND ra.isActive = true")
    Long countActiveByRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT COUNT(ra) FROM RestaurantAgency ra WHERE ra.agency.id = :agencyId AND ra.isActive = true AND ra.isApproved = true")
    Long countApprovedByAgency(@Param("agencyId") Long agencyId);

    // ==================== TOP AGENCIES ====================

    @Query("SELECT ra FROM RestaurantAgency ra WHERE ra.restaurant.id = :restaurantId " +
           "AND ra.isActive = true AND ra.isApproved = true " +
           "ORDER BY ra.totalBookings DESC")
    List<RestaurantAgency> findTopAgenciesByRestaurant(
        @Param("restaurantId") Long restaurantId, 
        Pageable pageable);
}
