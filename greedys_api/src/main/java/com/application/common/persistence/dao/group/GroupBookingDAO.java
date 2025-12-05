package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.GroupBooking;
import com.application.common.persistence.model.group.enums.BookerType;
import com.application.common.persistence.model.group.enums.GroupBookingStatus;
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
 * DAO per GroupBooking - Prenotazioni di gruppo
 */
@Repository
public interface GroupBookingDAO extends JpaRepository<GroupBooking, Long> {

    // ==================== BY CONFIRMATION CODE ====================

    Optional<GroupBooking> findByConfirmationCode(String confirmationCode);

    // ==================== BY RESTAURANT ====================

    Page<GroupBooking> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') " +
           "ORDER BY gb.eventDate ASC, gb.eventTime ASC")
    List<GroupBooking> findActiveByRestaurant(@Param("restaurantId") Long restaurantId);

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.status = :status ORDER BY gb.eventDate ASC")
    Page<GroupBooking> findByRestaurantAndStatus(
        @Param("restaurantId") Long restaurantId, 
        @Param("status") GroupBookingStatus status, 
        Pageable pageable);

    // ==================== BY AGENCY ====================

    Page<GroupBooking> findByAgencyId(Long agencyId, Pageable pageable);

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.agency.id = :agencyId " +
           "AND gb.status NOT IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') " +
           "ORDER BY gb.eventDate ASC")
    List<GroupBooking> findActiveByAgency(@Param("agencyId") Long agencyId);

    // ==================== BY BOOKER ====================

    Page<GroupBooking> findByBookerId(Long bookerId, Pageable pageable);

    List<GroupBooking> findByBookerIdAndBookerType(Long bookerId, BookerType bookerType);

    // ==================== BY DATE ====================

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.eventDate = :date ORDER BY gb.eventTime ASC")
    List<GroupBooking> findByRestaurantAndDate(
        @Param("restaurantId") Long restaurantId, 
        @Param("date") LocalDate date);

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY gb.eventDate ASC, gb.eventTime ASC")
    List<GroupBooking> findByRestaurantAndDateRange(
        @Param("restaurantId") Long restaurantId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);

    // ==================== UPCOMING ====================

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.eventDate >= :today " +
           "AND gb.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "ORDER BY gb.eventDate ASC, gb.eventTime ASC")
    List<GroupBooking> findUpcomingByRestaurant(
        @Param("restaurantId") Long restaurantId, 
        @Param("today") LocalDate today);

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.agency.id = :agencyId " +
           "AND gb.eventDate >= :today " +
           "AND gb.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "ORDER BY gb.eventDate ASC")
    List<GroupBooking> findUpcomingByAgency(
        @Param("agencyId") Long agencyId, 
        @Param("today") LocalDate today);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(gb) FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.status = :status")
    Long countByRestaurantAndStatus(
        @Param("restaurantId") Long restaurantId, 
        @Param("status") GroupBookingStatus status);

    @Query("SELECT SUM(gb.totalPax) FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND gb.eventDate = :date AND gb.status NOT IN ('CANCELLED', 'NO_SHOW')")
    Integer sumPaxByRestaurantAndDate(
        @Param("restaurantId") Long restaurantId, 
        @Param("date") LocalDate date);

    @Query("SELECT COUNT(gb) FROM GroupBooking gb WHERE gb.agency.id = :agencyId " +
           "AND gb.status = 'COMPLETED'")
    Long countCompletedByAgency(@Param("agencyId") Long agencyId);

    // ==================== SEARCH ====================

    @Query("SELECT gb FROM GroupBooking gb WHERE gb.restaurant.id = :restaurantId " +
           "AND (LOWER(gb.eventName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(gb.contactName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR gb.confirmationCode LIKE CONCAT('%', :search, '%'))")
    Page<GroupBooking> searchByRestaurant(
        @Param("restaurantId") Long restaurantId, 
        @Param("search") String search, 
        Pageable pageable);
}
