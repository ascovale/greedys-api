package com.application.admin.persistence.dao;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.AdminVerificationToken;
@Repository
public interface AdminVerificationTokenDAO extends JpaRepository<AdminVerificationToken, Long> {

    AdminVerificationToken findByToken(String token);

    AdminVerificationToken findByAdmin(Admin admin);

    Stream<AdminVerificationToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from AdminVerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
	