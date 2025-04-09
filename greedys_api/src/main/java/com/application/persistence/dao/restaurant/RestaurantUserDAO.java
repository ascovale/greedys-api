package com.application.persistence.dao.restaurant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    RestaurantUser findByEmail(String email);

    RestaurantUser findFirstByEmailOrderByIdAsc(@Param("email") String email);

    @Query("SELECT COUNT(ru) > 1 FROM RestaurantUser ru WHERE ru.email = :email")
    boolean isMultiRestaurantUser(@Param("email") String email);
    @Query("SELECT COUNT(ru) > 1 FROM RestaurantUser ru WHERE ru.email = (SELECT email FROM RestaurantUser WHERE id = :restaurantUserId)")
    boolean isMultiRestaurantUser(@Param("restaurantUserId") Long restaurantUserId);

    Optional<RestaurantUser> findRestaurantUserByIdAndEmail(Long userId, String email);
    @Query("SELECT ru FROM RestaurantUser ru WHERE ru.email = :email")
    List<RestaurantUser> findAllByEmail(@Param("email") String email);
}