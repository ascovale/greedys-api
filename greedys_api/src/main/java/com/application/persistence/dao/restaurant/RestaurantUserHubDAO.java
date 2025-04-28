package com.application.persistence.dao.restaurant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantUserHub;

@Repository
public interface RestaurantUserHubDAO extends JpaRepository<RestaurantUserHub, Long>{

    @Modifying
    @Query("UPDATE RestaurantUserHub ruh SET ruh.accepted = true WHERE ruh.id = :id")
    void acceptRestaurantUserHub(@Param("id") Long id);

    RestaurantUserHub findByEmail(@Param("email") String email);

    @Query("SELECT EXISTS (" +
           "SELECT 1 FROM RestaurantUser ru " +
           "WHERE ru.restaurantUserHub.id = :hubId AND ru.restaurant.id = :restaurantId)")
    boolean hasPermissionForRestaurant(@Param("hubId") Long hubId, @Param("restaurantId") Long restaurantId);
    
    @Query("SELECT ru.restaurant FROM RestaurantUser ru WHERE ru.restaurantUserHub.id = :hubId")
    List<Restaurant> findAllRestaurantsByHubId(@Param("hubId") Long hubId);
}