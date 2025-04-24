package com.application.persistence.dao.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantUser;

@Repository
public interface RestaurantUserDAO extends JpaRepository<RestaurantUser, Long>{

    @Modifying
    @Query("UPDATE RestaurantUser ru SET ru.accepted = true WHERE ru.id = :id")
    void acceptRestaurantUser(@Param("id") Long id);

    Collection<RestaurantUser> findByRestaurantId(Long id);

    @Query("SELECT ru FROM RestaurantUser ru WHERE ru.restaurantUserHub.email = :email")
    RestaurantUser findByEmail(@Param("email") String email);

    @Query("SELECT ru FROM RestaurantUser ru WHERE ru.restaurantUserHub.email = :email")
    List<RestaurantUser> findAllByEmail(@Param("email") String email);

    @Query("SELECT ru FROM RestaurantUser ru WHERE ru.restaurantUserHub.email = :email AND ru.restaurant.id = :restaurantId")
    RestaurantUser findByEmailAndRestaurantId(@Param("email") String email, @Param("restaurantId") Long restaurantId);
}