package com.application.persistence.dao.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantUserHub;

@Repository
public interface RestaurantUserHubDAO extends JpaRepository<RestaurantUserHub, Long>{

    @Modifying
    @Query("UPDATE RestaurantUserHub ruh SET ruh.accepted = true WHERE ruh.id = :id")
    void acceptRestaurantUserHub(@Param("id") Long id);

    RestaurantUserHub findByEmail(@Param("email") String email);
}