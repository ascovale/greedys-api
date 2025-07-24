package com.application.restaurant.persistence.dao;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.user.RUser;

@Repository
public interface RUserDAO extends JpaRepository<RUser, Long>{

    @Modifying
    @Query("UPDATE RUser ru SET ru.status = :status WHERE ru.id = :id")
    void setUserStatus(@Param("id") Long id, @Param("status") RUser.Status status);

    Collection<RUser> findByRestaurantId(Long id);

    @Query("SELECT ru FROM RUser ru WHERE ru.RUserHub.email = :email")
    RUser findByEmail(@Param("email") String email);

    @Query("SELECT ru FROM RUser ru WHERE ru.RUserHub.email = :email")
    List<RUser> findAllByEmail(@Param("email") String email);

    @Query("SELECT ru FROM RUser ru WHERE ru.RUserHub.email = :email AND ru.restaurant.id = :restaurantId")
    RUser findByEmailAndRestaurantId(@Param("email") String email, @Param("restaurantId") Long restaurantId);

    @Query("SELECT ru FROM RUser ru WHERE ru.RUserHub.id = :hubId")
    List<RUser> findAllByRUserHubId(@Param("hubId") Long hubId);



}