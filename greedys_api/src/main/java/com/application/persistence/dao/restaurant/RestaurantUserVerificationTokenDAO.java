package com.application.persistence.dao.restaurant;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserVerificationToken;
@Repository
public interface RestaurantUserVerificationTokenDAO extends JpaRepository<RestaurantUserVerificationToken, Long> {

    RestaurantUserVerificationToken findByToken(String token);

    RestaurantUserVerificationToken findByRestaurantUser(RestaurantUser restaurantUser);
    
    Stream<RestaurantUserVerificationToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from RestaurantUserVerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
	