package com.application.restaurant.persistence.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.customer.persistence.model.Customer;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantReservation;

/**
 * DAO for RestaurantReservation entity - manages restaurant customer agenda
 */
@Repository
public interface RestaurantReservationDAO extends JpaRepository<RestaurantReservation, Long> {

    /**
     * Find relationship between restaurant and customer
     */
    Optional<RestaurantReservation> findByRestaurantAndCustomer(Restaurant restaurant, Customer customer);

    /**
     * Find all customers in restaurant's agenda
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant = :restaurant ORDER BY rr.lastInteraction DESC")
    Page<RestaurantReservation> findByRestaurant(@Param("restaurant") Restaurant restaurant, Pageable pageable);

    /**
     * Find customers by restaurant ID
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId ORDER BY rr.lastInteraction DESC")
    Page<RestaurantReservation> findByRestaurantId(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Search customers in restaurant's agenda by name or contact info
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND (LOWER(rr.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(rr.customer.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR rr.customer.phoneNumber LIKE CONCAT('%', :searchTerm, '%') " +
           "OR LOWER(rr.customer.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY rr.lastInteraction DESC")
    Page<RestaurantReservation> searchCustomersInAgenda(@Param("restaurantId") Long restaurantId, 
                                                        @Param("searchTerm") String searchTerm, 
                                                        Pageable pageable);

    /**
     * Find favorite customers for restaurant
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND rr.isFavorite = true ORDER BY rr.totalReservations DESC")
    Page<RestaurantReservation> findFavoriteCustomers(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Find regular customers (3+ reservations)
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND rr.totalReservations >= :minReservations ORDER BY rr.totalReservations DESC")
    Page<RestaurantReservation> findRegularCustomers(@Param("restaurantId") Long restaurantId, 
                                                     @Param("minReservations") Integer minReservations, 
                                                     Pageable pageable);

    /**
     * Find new customers (first interaction within timeframe)
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND rr.firstInteraction >= :since ORDER BY rr.firstInteraction DESC")
    Page<RestaurantReservation> findNewCustomers(@Param("restaurantId") Long restaurantId, 
                                                 @Param("since") Instant since, 
                                                 Pageable pageable);

    /**
     * Find blacklisted customers
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND rr.isBlacklisted = true ORDER BY rr.updatedAt DESC")
    List<RestaurantReservation> findBlacklistedCustomers(@Param("restaurantId") Long restaurantId);

    /**
     * Count total customers in restaurant agenda
     */
    @Query("SELECT COUNT(rr) FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId")
    Long countCustomersByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Count customers by status
     */
    @Query("SELECT COUNT(rr) FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId AND rr.isFavorite = :isFavorite")
    Long countFavoriteCustomers(@Param("restaurantId") Long restaurantId, @Param("isFavorite") Boolean isFavorite);

    @Query("SELECT COUNT(rr) FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId AND rr.totalReservations >= :minReservations")
    Long countRegularCustomers(@Param("restaurantId") Long restaurantId, @Param("minReservations") Integer minReservations);

    @Query("SELECT COUNT(rr) FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId AND rr.firstInteraction >= :since")
    Long countNewCustomers(@Param("restaurantId") Long restaurantId, @Param("since") Instant since);

    /**
     * Find customers with notes
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND rr.customerNotes IS NOT NULL AND rr.customerNotes != '' " +
           "ORDER BY rr.updatedAt DESC")
    Page<RestaurantReservation> findCustomersWithNotes(@Param("restaurantId") Long restaurantId, Pageable pageable);

    /**
     * Find customers by contact source
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND rr.contactSource = :source ORDER BY rr.firstInteraction DESC")
    Page<RestaurantReservation> findByContactSource(@Param("restaurantId") Long restaurantId, 
                                                    @Param("source") String source, 
                                                    Pageable pageable);

    /**
     * Find customers without recent activity
     */
    @Query("SELECT rr FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId " +
           "AND (rr.lastInteraction IS NULL OR rr.lastInteraction < :cutoffDate) " +
           "ORDER BY rr.lastInteraction ASC")
    Page<RestaurantReservation> findInactiveCustomers(@Param("restaurantId") Long restaurantId, 
                                                      @Param("cutoffDate") Instant cutoffDate, 
                                                      Pageable pageable);

    /**
     * Get customer statistics for restaurant
     */
    @Query("SELECT " +
           "COUNT(rr) as totalCustomers, " +
           "SUM(rr.totalReservations) as totalReservations, " +
           "SUM(rr.totalGuests) as totalGuests, " +
           "AVG(rr.totalReservations) as avgReservationsPerCustomer, " +
           "AVG(rr.totalGuests) as avgGuestsPerCustomer " +
           "FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId")
    Object[] getCustomerStatistics(@Param("restaurantId") Long restaurantId);

    /**
     * Check if customer exists in restaurant agenda
     */
    @Query("SELECT COUNT(rr) > 0 FROM RestaurantReservation rr WHERE rr.restaurant.id = :restaurantId AND rr.customer.id = :customerId")
    Boolean existsByRestaurantIdAndCustomerId(@Param("restaurantId") Long restaurantId, @Param("customerId") Long customerId);
}