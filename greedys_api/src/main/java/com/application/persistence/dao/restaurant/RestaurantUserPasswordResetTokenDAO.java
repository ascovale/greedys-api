package com.application.persistence.dao.restaurant;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserPasswordResetToken;

@Repository
public interface RestaurantUserPasswordResetTokenDAO extends JpaRepository<RestaurantUserPasswordResetToken, Long> {

    RestaurantUserPasswordResetToken findByToken(String token);

    RestaurantUserPasswordResetToken findByRestaurantUser(RestaurantUser restaurantUser);

    Stream<RestaurantUserPasswordResetToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from RestaurantUserPasswordResetToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
