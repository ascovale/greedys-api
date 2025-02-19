package com.application.persistence.dao.customer;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.user.PasswordResetToken;
import com.application.persistence.model.user.Customer;

@Repository
public interface PasswordResetTokenDAO extends JpaRepository<PasswordResetToken, Long> {

    PasswordResetToken findByToken(String token);

    PasswordResetToken findByUser(Customer user);

    Stream<PasswordResetToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
