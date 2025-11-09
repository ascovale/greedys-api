package com.application.restaurant.persistence.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.RestaurantSettings;

/**
 * DAO for RestaurantSettings entity
 */
@Repository
public interface RestaurantSettingsDAO extends JpaRepository<RestaurantSettings, Long> {

    /**
     * Find restaurant settings by restaurant UUID
     */
    Optional<RestaurantSettings> findByRestaurantId(UUID restaurantId);

    /**
     * Find restaurant settings by restaurant entity ID
     */
    @Query("SELECT rs FROM RestaurantSettings rs WHERE rs.restaurant.id = :restaurantEntityId")
    Optional<RestaurantSettings> findByRestaurantEntityId(@Param("restaurantEntityId") Long restaurantEntityId);

    /**
     * Check if restaurant has custom settings
     */
    boolean existsByRestaurantId(UUID restaurantId);

    /**
     * Delete settings by restaurant UUID
     */
    void deleteByRestaurantId(UUID restaurantId);

    /**
     * Find all settings with custom form schema
     */
    @Query("SELECT rs FROM RestaurantSettings rs WHERE rs.customerFormSchema IS NOT NULL")
    java.util.List<RestaurantSettings> findAllWithCustomSchema();

    /**
     * Find all settings with custom matching policy
     */
    @Query("SELECT rs FROM RestaurantSettings rs WHERE rs.matchingPolicy IS NOT NULL")
    java.util.List<RestaurantSettings> findAllWithCustomMatchingPolicy();
}