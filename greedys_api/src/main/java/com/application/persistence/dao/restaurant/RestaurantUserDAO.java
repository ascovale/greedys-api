package com.application.persistence.dao.restaurant;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.RestaurantUser;

@Repository
public interface RestaurantUserDAO extends JpaRepository<RestaurantUser, Long>{

    @Modifying
    @Query("UPDATE RestaurantUser ru SET ru.accepted = true WHERE ru.id = :id")
    void acceptUser(@Param("id") Long id);

    Collection<RestaurantUser> findByRestaurantId(Long id);   
} 