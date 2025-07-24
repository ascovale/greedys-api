package com.application.restaurant.persistence.dao.menu;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.menu.Menu;

@Repository
public interface MenuDAO extends JpaRepository<Menu, Long> {
    @Query("""
        SELECT m FROM Menu m JOIN m.services s JOIN s.restaurant r 
        WHERE r.id = :restaurantId
    """)
    List<Menu> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("""
        SELECT DISTINCT m FROM Menu m JOIN m.services s JOIN s.restaurant r 
        WHERE r.id = :restaurantId 
        AND CURRENT_DATE BETWEEN s.validFrom AND s.validTo 
        AND s.enabled = true 
        AND m.enabled = true
    """)
    List<Menu> findMenusWithActiveServicesByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("""
        SELECT DISTINCT m FROM Menu m JOIN m.services s JOIN s.restaurant r 
        WHERE r.id = :restaurantId 
        AND :date BETWEEN s.validFrom AND s.validTo 
        AND s.enabled = true 
        AND m.enabled = true
    """)
    List<Menu> findMenusWithActiveServicesByRestaurantIdAndDate(@Param("restaurantId") Long restaurantId, @Param("date") LocalDate date);

    @Query("""
        SELECT DISTINCT m FROM Menu m JOIN m.services s JOIN s.restaurant r 
        WHERE r.id = :restaurantId 
        AND s.enabled = true 
        AND m.enabled = true 
        AND s.validFrom <= :endDate 
        AND s.validTo >= :startDate
    """)
    List<Menu> findMenusWithServicesValidInPeriod(@Param("restaurantId") Long restaurantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT DISTINCT m FROM Menu m JOIN m.services s 
        WHERE s.id = :serviceId 
        AND s.enabled = true 
        AND m.enabled = true 
        AND :date BETWEEN s.validFrom AND s.validTo
    """)
    List<Menu> findActiveEnabledMenusByServiceIdAndDate(@Param("serviceId") Long serviceId, @Param("date") LocalDate date);

    @Query("""
        SELECT DISTINCT m FROM Menu m JOIN m.services s 
        WHERE s.id = :serviceId 
        AND s.enabled = true 
        AND m.enabled = true 
        AND s.validFrom <= :endDate 
        AND s.validTo >= :startDate
    """)
    List<Menu> findActiveEnabledMenusByServiceIdAndPeriod(@Param("serviceId") Long serviceId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}