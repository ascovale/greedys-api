package com.application.customer.persistence.dao;

import java.time.LocalDate;
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

    @Query(value = "SELECT r FROM Reservation r WHERE r.slot.service.restaurant.id = :restaurantId AND r.date = :reservationDate")
    List<Reservation> findDayReservation(Long restaurantId, LocalDate reservationDate);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date BETWEEN :startDate AND :endDate
                ORDER BY r.date, r.slot.start
            """)
    Collection<Reservation> findByRestaurantAndDateBetween(Long restaurantId, LocalDate startDate, LocalDate endDate);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date = :reservationDate
                ORDER BY r.slot.start
            """)
    Collection<Reservation> findByRestaurantAndDate(Long restaurantId, LocalDate reservationDate);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date BETWEEN :startDate AND :endDate
                AND r.status = :status
                ORDER BY r.date, r.slot.start
            """)
    Collection<Reservation> findByRestaurantAndDateBetweenAndStatus(Long restaurantId, LocalDate startDate, LocalDate endDate, Reservation.Status status);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date >= :startDate
                AND r.status = :status
                ORDER BY r.date, r.slot.start
            """)
    Collection<Reservation> findByRestaurantAndDateAndStatus(Long restaurantId, LocalDate startDate, Reservation.Status status);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.status = :status
                ORDER BY r.createdAt, r.date, r.slot.start
            """)
    Collection<Reservation> findByRestaurantIdAndStatus(Long restaurantId, Reservation.Status status);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer IS NOT NULL 
                AND r.customer.id = :customerId
                AND r.status = :status
                ORDER BY r.date, r.slot.start
            """)
    Collection<Reservation> findByCustomerAndStatus(Long customerId, Reservation.Status status);

    Collection<Reservation> findBySlot_Id(Long slotId);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.slot.id = :slotId 
                AND r.date >= :fromDate
                ORDER BY r.date, r.slot.start
            """)
    List<Reservation> findFutureReservationsBySlotId(Long slotId, LocalDate fromDate);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date BETWEEN :startDate AND :endDate
                AND r.status = :status
                ORDER BY r.date, r.slot.start
            """)
    Page<Reservation> findByRestaurantAndDateBetweenAndStatus(Long restaurantId, LocalDate startDate, LocalDate endDate, Reservation.Status status, Pageable pageable);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date >= :startDate
                AND r.status = :status
                ORDER BY r.date, r.slot.start
            """)
    Page<Reservation> findByRestaurantAndDateAndStatus(Long restaurantId, LocalDate startDate, Reservation.Status status, Pageable pageable);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer IS NOT NULL 
                AND r.customer.id = :customerId
                AND r.status = :status
                ORDER BY r.date, r.slot.start
            """)
    Page<Reservation> findByCustomerAndStatus(Long customerId, Reservation.Status status, Pageable pageable);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.restaurant.id = :restaurantId
                AND r.date BETWEEN :startDate AND :endDate
                ORDER BY r.date, r.slot.start
            """)
    Page<Reservation> findReservationsByRestaurantAndDateRange(Long restaurantId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer IS NOT NULL AND r.customer.id = :customerId
            ORDER BY r.date DESC, r.slot.start
            """)
    Collection<Reservation> findByCustomer(Long customerId);


    @Query(value = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.customer.id = :customerId
            """)
    Integer countByCustomer(Long customerId);

    @Query(value = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.customer.id = :customerId AND r.status = :status
            """)
    Integer countByCustomerAndStatus(Long customerId, Reservation.Status status);

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
    List<com.application.customer.persistence.model.Customer> findCustomersByRestaurantId(Long restaurantId);

    /**
     * Find customers with pagination
     */
    @Query(value = """
            SELECT DISTINCT r.customer FROM Reservation r
            WHERE r.restaurant.id = :restaurantId 
                AND r.customer IS NOT NULL
            """)
    Page<com.application.customer.persistence.model.Customer> findCustomersByRestaurantIdPageable(Long restaurantId, Pageable pageable);

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
    List<com.application.customer.persistence.model.Customer> searchCustomersByRestaurantId(Long restaurantId, String searchTerm);

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
    List<com.application.customer.persistence.model.Customer> findUnregisteredCustomersByRestaurantId(Long restaurantId);

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
            ORDER BY r.date DESC, r.slot.start DESC
            """)
    List<Reservation> findByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);

    /**
     * Get all reservations for a specific customer at a specific restaurant with pagination
     */
    @Query(value = """
            SELECT r FROM Reservation r
            WHERE r.customer.id = :customerId 
                AND r.restaurant.id = :restaurantId
            ORDER BY r.date DESC, r.slot.start DESC
            """)
    Page<Reservation> findByCustomerIdAndRestaurantId(Long customerId, Long restaurantId, Pageable pageable);
}
