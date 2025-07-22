package com.application.admin.dao;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.admin.model.Admin;
import com.application.admin.model.AdminPasswordResetToken;

@Repository
public interface AdminPasswordResetTokenDAO extends JpaRepository<AdminPasswordResetToken, Long> {

    AdminPasswordResetToken findByToken(String token);

    AdminPasswordResetToken findByAdmin(Admin admin);

    Stream<AdminPasswordResetToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from AdminPasswordResetToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
