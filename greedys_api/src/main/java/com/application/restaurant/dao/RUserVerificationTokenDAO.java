package com.application.restaurant.dao;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.user.RUser;
import com.application.restaurant.model.user.RUserVerificationToken;
@Repository
public interface RUserVerificationTokenDAO extends JpaRepository<RUserVerificationToken, Long> {

    RUserVerificationToken findByToken(String token);

    @Query("SELECT v FROM RUserVerificationToken v WHERE v.rUser = :rUser")
    RUserVerificationToken findByRUser(@Param("rUser") RUser rUser);
    
    Stream<RUserVerificationToken> findAllByExpiryDateLessThan(LocalDateTime now);

    void deleteByExpiryDateLessThan(LocalDateTime now);

    @Modifying
    @Query("delete from RUserVerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime now);
}
	