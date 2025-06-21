package com.application.persistence.dao.restaurant;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RUser;
import com.application.persistence.model.restaurant.user.RUserPasswordResetToken;

@Repository
public interface RUserPasswordResetTokenDAO extends JpaRepository<RUserPasswordResetToken, Long> {

    RUserPasswordResetToken findByToken(String token);

    RUserPasswordResetToken findByRUser(RUser RUser);

    Stream<RUserPasswordResetToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from RUserPasswordResetToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
