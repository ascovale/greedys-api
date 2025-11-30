package com.application.customer.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.reservation.ReservationModificationRequest;

/**
 * DAO for ReservationModificationRequest entity
 * 
 * Handles all database operations for reservation modification requests
 */
@Repository
public interface ReservationModificationRequestDAO extends JpaRepository<ReservationModificationRequest, Long> {

    /**
     * Find all pending modification requests for a specific reservation
     */
    @Query("SELECT r FROM ReservationModificationRequest r WHERE r.reservation.id = :reservationId AND r.status = 'PENDING_APPROVAL'")
    List<ReservationModificationRequest> findPendingByReservationId(@Param("reservationId") Long reservationId);

    /**
     * Find all modification requests for a specific reservation
     */
    List<ReservationModificationRequest> findByReservationId(Long reservationId);

    /**
     * Find all pending modification requests for a restaurant
     */
    @Query("SELECT r FROM ReservationModificationRequest r WHERE r.reservation.restaurant.id = :restaurantId AND r.status = 'PENDING_APPROVAL' ORDER BY r.requestedAt DESC")
    List<ReservationModificationRequest> findPendingByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Find modification request by ID
     */
    Optional<ReservationModificationRequest> findById(Long id);

    /**
     * Find all modification requests with specific status for a restaurant
     */
    @Query("SELECT r FROM ReservationModificationRequest r WHERE r.reservation.restaurant.id = :restaurantId AND r.status = :status ORDER BY r.requestedAt DESC")
    List<ReservationModificationRequest> findByRestaurantIdAndStatus(
            @Param("restaurantId") Long restaurantId, 
            @Param("status") ReservationModificationRequest.Status status);
}
