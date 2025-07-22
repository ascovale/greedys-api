package com.application.restaurant.dao;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.user.RUser;
import com.application.restaurant.model.user.RUserPasswordResetToken;

@Repository
public interface RUserPasswordResetTokenDAO extends JpaRepository<RUserPasswordResetToken, Long> {

    RUserPasswordResetToken findByToken(String token);

    @Query("SELECT p FROM RUserPasswordResetToken p WHERE p.rUser = :rUser")
    RUserPasswordResetToken findByRUser(@Param("rUser") RUser rUser);

    Stream<RUserPasswordResetToken> findAllByExpiryDateLessThan(LocalDateTime now);

    void deleteByExpiryDateLessThan(LocalDateTime now);

    @Modifying
    @Query("delete from RUserPasswordResetToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime now);
}
