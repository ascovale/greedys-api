package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.FixedPriceMenu;
import com.application.common.persistence.model.group.enums.FixedPriceMenuType;
import com.application.common.persistence.model.group.enums.MenuVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * DAO per FixedPriceMenu - Menù a prezzo fisso
 */
@Repository
public interface FixedPriceMenuDAO extends JpaRepository<FixedPriceMenu, Long> {

    // ==================== BY RESTAURANT ====================

    List<FixedPriceMenu> findByRestaurantIdAndEnabledTrue(Long restaurantId);

    Page<FixedPriceMenu> findByRestaurantId(Long restaurantId, Pageable pageable);

    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId AND m.enabled = true ORDER BY m.name ASC")
    List<FixedPriceMenu> findActiveByRestaurantOrdered(@Param("restaurantId") Long restaurantId);

    // ==================== BY VISIBILITY ====================

    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId " +
           "AND m.enabled = true AND m.visibility IN :visibilities")
    List<FixedPriceMenu> findByRestaurantAndVisibilities(
        @Param("restaurantId") Long restaurantId, 
        @Param("visibilities") List<MenuVisibility> visibilities);

    /**
     * Menù visibili ai Customer (CUSTOMER_ONLY o BOTH)
     */
    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId " +
           "AND m.enabled = true AND (m.visibility = 'CUSTOMER_ONLY' OR m.visibility = 'BOTH')")
    List<FixedPriceMenu> findCustomerMenus(@Param("restaurantId") Long restaurantId);

    /**
     * Menù visibili alle Agency (AGENCY_ONLY o BOTH)
     */
    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId " +
           "AND m.enabled = true AND (m.visibility = 'AGENCY_ONLY' OR m.visibility = 'BOTH')")
    List<FixedPriceMenu> findAgencyMenus(@Param("restaurantId") Long restaurantId);

    // ==================== BY TYPE ====================

    List<FixedPriceMenu> findByRestaurantIdAndMenuTypeAndEnabledTrue(Long restaurantId, FixedPriceMenuType menuType);

    // ==================== BY PAX ====================

    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId " +
           "AND m.enabled = true " +
           "AND (m.minimumPax IS NULL OR m.minimumPax <= :pax) " +
           "AND (m.maximumPax IS NULL OR m.maximumPax >= :pax)")
    List<FixedPriceMenu> findAvailableForPax(
        @Param("restaurantId") Long restaurantId, 
        @Param("pax") Integer pax);

    // ==================== BY VALIDITY ====================

    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId " +
           "AND m.enabled = true " +
           "AND (m.validFrom IS NULL OR m.validFrom <= :date) " +
           "AND (m.validTo IS NULL OR m.validTo >= :date)")
    List<FixedPriceMenu> findValidOnDate(
        @Param("restaurantId") Long restaurantId, 
        @Param("date") LocalDate date);

    // ==================== SEARCH ====================

    @Query("SELECT m FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId " +
           "AND m.enabled = true " +
           "AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<FixedPriceMenu> searchMenus(
        @Param("restaurantId") Long restaurantId, 
        @Param("search") String search, 
        Pageable pageable);

    // ==================== STATISTICS ====================

    @Query("SELECT COUNT(m) FROM FixedPriceMenu m WHERE m.restaurant.id = :restaurantId AND m.enabled = true")
    Long countActiveByRestaurant(@Param("restaurantId") Long restaurantId);
}
