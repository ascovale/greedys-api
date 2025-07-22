package com.application.restaurant.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.Restaurant;
import com.application.restaurant.model.user.RUserHub;

@Repository
public interface RUserHubDAO extends JpaRepository<RUserHub, Long>{

    @Modifying
    @Query("UPDATE RUserHub ruh SET ruh.accepted = true WHERE ruh.id = :id")
    void acceptRUserHub(@Param("id") Long id);

    RUserHub findByEmail(@Param("email") String email);

    @Query("SELECT EXISTS (" +
           "SELECT 1 FROM RUser ru " +
           "WHERE ru.RUserHub.id = :hubId AND ru.restaurant.id = :restaurantId)")
    boolean hasPermissionForRestaurant(@Param("hubId") Long hubId, @Param("restaurantId") Long restaurantId);
    
    @Query("SELECT ru.restaurant FROM RUser ru WHERE ru.RUserHub.id = :hubId")
    List<Restaurant> findAllRestaurantsByHubId(@Param("hubId") Long hubId);

    @Query("SELECT ru.restaurant FROM RUser ru WHERE ru.RUserHub.email = :email")
    List<Restaurant> findAllRestaurantsByHubEmail(@Param("email") String email);


}