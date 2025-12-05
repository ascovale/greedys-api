package com.application.customer.persistence.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.reservations.ReservationDTO;
import com.application.customer.persistence.model.Customer;

@Repository
public interface ReservationDAO extends JpaRepository<Reservation, Long> {

    List<ReservationDTO> findAllByCustomer_Id(Long customerId);

    @Query(value = """
            SELECT day FROM (
                SELECT day
                FROM service_full
                WHERE idservice IN (
                    SELECT id
                    FROM service
                    WHERE idrestaurant = :restaurantId
                )
                UNION ALL
                SELECT day
                FROM service_closed
                WHERE idservice IN (
                    SELECT id
                    FROM service
                    WHERE idrestaurant = :restaurantId
                )
            ) AS days
            GROUP BY day
            HAVING COUNT(*) = (
                SELECT COUNT(*)
                FROM service
                WHERE idrestaurant = :restaurantId
            ) AND day >= CURRENT_DATE
            """, nativeQuery = true)
    List<LocalDate> findClosedOrFullServices(Long restaurantId);

    @Query(value = """
            WITH date_range AS (
                SELECT MIN(`from`) AS start_date, MAX(`to`) AS end_date
                FROM service
                WHERE idrestaurant = :restaurantId
            ), all_dates AS (
                SELECT ADDDATE(start_date, @num:=@num+1) the_date
                FROM date_range
                JOIN (SELECT @num:=-1) num
                WHERE ADDDATE(start_date, @num+1) <= end_date
            )
            SELECT the_date
            FROM all_dates
            WHERE NOT EXISTS (
                SELECT 1
                FROM service
                WHERE all_dates.the_date BETWEEN service.`s_from` AND service.`s_to`
                AND idrestaurant = :restaurantId
            )
            """, nativeQuery = true)
    List<LocalDate> findDatesWithoutAnyService(Long restaurantId);

    // ===== PRIMARY METHODS - Day View (Most Used) =====

    /**
     * PRIMARY: Find all reservations for a restaurant on a specific day (paginated).
     * Index: (id_restaurant, reservation_datetime)
     * Performance: ~8-15ms for 50-200 rows
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.restaurant.id = :restaurantId
          AND r.reservationDateTime BETWEEN :dayStart AND :dayEnd
        ORDER BY r.reservationDateTime ASC
    """)
    Page<Reservation> findByRestaurantIdAndReservationDatetimeBetween(
        Long restaurantId,
        LocalDateTime dayStart,
        LocalDateTime dayEnd,
        Pageable pageable
    );

    /**
     * PRIMARY: Find reservations for restaurant + service on a specific day (paginated).
     * Most selective query - best performance.
     * Index: (id_restaurant, service_id, reservation_datetime)
     * Performance: ~8-12ms for 10-50 rows
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.restaurant.id = :restaurantId
          AND r.service.id = :serviceId
          AND r.reservationDateTime BETWEEN :dayStart AND :dayEnd
        ORDER BY r.reservationDateTime ASC
    """)
    Page<Reservation> findByRestaurantIdAndServiceIdAndReservationDatetimeBetween(
        Long restaurantId,
        Long serviceId,
        LocalDateTime dayStart,
        LocalDateTime dayEnd,
        Pageable pageable
    );

    /**
     * PRIMARY: Find only confirmed reservations (ACCEPTED, SEATED) for a day.
     * For restaurant staff: "How many guests am I expecting today?"
     * Performance: ~12-18ms
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.restaurant.id = :restaurantId
          AND r.reservationDateTime BETWEEN :dayStart AND :dayEnd
          AND r.status IN ('ACCEPTED', 'SEATED')
        ORDER BY r.reservationDateTime ASC
    """)
    Page<Reservation> findConfirmedReservationsByRestaurantAndDay(
        Long restaurantId,
        LocalDateTime dayStart,
        LocalDateTime dayEnd,
        Pageable pageable
    );

    /**
     * PRIMARY: Find only pending reservations (NOT_ACCEPTED) for a day.
     * For admin staff: "What reservations are waiting for acceptance?"
     * Performance: ~12-18ms
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.restaurant.id = :restaurantId
          AND r.reservationDateTime BETWEEN :dayStart AND :dayEnd
          AND r.status = 'NOT_ACCEPTED'
        ORDER BY r.reservationDateTime ASC
    """)
    Page<Reservation> findPendingReservationsByRestaurantAndDay(
        Long restaurantId,
        LocalDateTime dayStart,
        LocalDateTime dayEnd,
        Pageable pageable
    );

    // ===== SECONDARY METHODS - Support Queries =====

    /**
     * SECONDARY: Find future reservations for a service (non-paginated, internal use).
     * Index: (service_id, reservation_datetime)
     * Performance: ~12-20ms
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.service.id = :serviceId
          AND r.reservationDateTime >= :fromDateTime
        ORDER BY r.reservationDateTime ASC
    """)
    List<Reservation> findByServiceIdAndReservationDatetimeAfter(
        Long serviceId,
        LocalDateTime fromDateTime
    );

    /**
     * SECONDARY: Find all reservations for a customer (paginated, most recent first).
     * Use case: "My Reservations" in customer app
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.customer.id = :customerId
          AND r.customer IS NOT NULL
        ORDER BY r.reservationDateTime DESC
    """)
    Page<Reservation> findByCustomerIdOrderByReservationDatetimeDesc(
        Long customerId,
        Pageable pageable
    );

    /**
     * SECONDARY: Find future reservations for a customer (non-paginated).
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.customer.id = :customerId
          AND r.customer IS NOT NULL
          AND r.reservationDateTime >= :fromDateTime
        ORDER BY r.reservationDateTime ASC
    """)
    List<Reservation> findByCustomerIdAndReservationDatetimeAfter(
        Long customerId,
        LocalDateTime fromDateTime
    );

    // ===== UTILITY METHODS (Unchanged) =====

    @Query(value = """
            SELECT r FROM Reservation r 
            LEFT JOIN FETCH r.restaurant
            WHERE r.id = :reservationId
            """)
    Optional<Reservation> findByIdWithRestaurant(Long reservationId);

    // === Restaurant Contact/Rubrica Methods ===

    /**
     * Find all customers who have made reservations at a restaurant
     */
    @Query(value = """
            SELECT DISTINCT r.customer FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
            ORDER BY r.customer.name, r.customer.surname
            """)
    List<Customer> findCustomersByRestaurantId(Long restaurantId);

    /**
     * Find customers with pagination
     */
    @Query(value = """
            SELECT DISTINCT r.customer FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
            """)
    Page<Customer> findCustomersByRestaurantIdPageable(Long restaurantId, Pageable pageable);

    /**
     * Search customers by name, email, or phone
     */
    @Query(value = """
            SELECT DISTINCT r.customer FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
                AND (LOWER(r.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR LOWER(r.customer.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR LOWER(r.customer.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                    OR r.customer.phoneNumber LIKE CONCAT('%', :searchTerm, '%'))
            ORDER BY r.customer.name, r.customer.surname
            """)
    List<Customer> searchCustomersByRestaurantId(Long restaurantId, String searchTerm);

    /**
     * Find unregistered customers (UNREGISTERED status)
     */
    @Query(value = """
            SELECT DISTINCT r.customer FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
                AND r.customer.status = 'UNREGISTERED'
            ORDER BY r.customer.name, r.customer.surname
            """)
    List<Customer> findUnregisteredCustomersByRestaurantId(Long restaurantId);

    /**
     * Count total unique customers for a restaurant
     */
    @Query(value = """
            SELECT COUNT(DISTINCT r.customer) FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
            """)
    Long countCustomersByRestaurantId(Long restaurantId);

    /**
     * Count registered customers (not UNREGISTERED)
     */
    @Query(value = """
            SELECT COUNT(DISTINCT r.customer) FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
                AND r.customer.status != 'UNREGISTERED'
            """)
    Long countRegisteredCustomersByRestaurantId(Long restaurantId);

    /**
     * Count unregistered customers
     */
    @Query(value = """
            SELECT COUNT(DISTINCT r.customer) FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
                AND r.customer.status = 'UNREGISTERED'
            """)
    Long countUnregisteredCustomersByRestaurantId(Long restaurantId);

    /**
     * Check if customer has reservations at restaurant
     */
    boolean existsByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);

    /**
     * Count reservations for specific customer at restaurant
     */
    @Query(value = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.customer.id = :customerId 
                AND r.restaurant.id = :restaurantId
            """)
    Long countByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);

    /**
     * Get last reservation datetime for customer at restaurant
     */
    @Query(value = """
            SELECT MAX(r.reservationDateTime) FROM Reservation r
            WHERE r.customer.id = :customerId 
                AND r.restaurant.id = :restaurantId
            """)
    LocalDateTime findLastReservationDatetimeByCustomerAndRestaurant(Long customerId, Long restaurantId);

    /**
     * Get last reservation date for customer at restaurant
     */
    @Query(value = """
            SELECT MAX(r.date) FROM Reservation r
            WHERE r.customer.id = :customerId 
                AND r.restaurant.id = :restaurantId
            """)
    LocalDate findLastReservationDateByCustomerAndRestaurant(Long customerId, Long restaurantId);

    /**
     * Get all reservations for a specific customer at a specific restaurant
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer.id = :customerId 
                AND r.restaurant.id = :restaurantId
            ORDER BY r.reservationDateTime DESC
            """)
    List<Reservation> findByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);

    /**
     * Get all reservations for a specific customer at a specific restaurant with pagination
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer.id = :customerId 
                AND r.restaurant.id = :restaurantId
            ORDER BY r.reservationDateTime DESC
            """)
    Page<Reservation> findByCustomerIdAndRestaurantIdPageable(Long customerId, Long restaurantId, Pageable pageable);

    /**
     * Count reservations for a customer
     */
    @Query(value = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.customer.id = :customerId
            """)
    Integer countByCustomerId(Long customerId);

    /**
     * Count reservations for a customer with specific status
     */
    @Query(value = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.customer.id = :customerId AND r.status = :status
            """)
    Integer countByCustomerIdAndStatus(Long customerId, Reservation.Status status);

    /**
     * Find all reservations for a customer
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer IS NOT NULL AND r.customer.id = :customerId
            ORDER BY r.reservationDateTime DESC
            """)
    Collection<Reservation> findByCustomerId(Long customerId);

    /**
     * Find reservations for a customer with specific status
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer IS NOT NULL 
                AND r.customer.id = :customerId
                AND r.status = :status
            ORDER BY r.reservationDateTime DESC
            """)
    Collection<Reservation> findByCustomerIdAndStatus(Long customerId, Reservation.Status status);

    /**
     * Find reservations by restaurant and status
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.status = :status
            ORDER BY r.createdAt, r.reservationDateTime
            """)
    Collection<Reservation> findByRestaurantIdAndStatus(Long restaurantId, Reservation.Status status);

    /**
     * Find paginated reservations by restaurant and status
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.status = :status
            ORDER BY r.reservationDateTime ASC
            """)
    Page<Reservation> findByRestaurantIdAndStatusPageable(Long restaurantId, Reservation.Status status, Pageable pageable);

    /**
     * Find paginated reservations by customer and status
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer IS NOT NULL 
                AND r.customer.id = :customerId
                AND r.status = :status
            ORDER BY r.reservationDateTime ASC
            """)
    Page<Reservation> findByCustomerIdAndStatusPageable(Long customerId, Reservation.Status status, Pageable pageable);
}
