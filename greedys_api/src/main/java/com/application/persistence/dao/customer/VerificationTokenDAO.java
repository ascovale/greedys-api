package com.application.persistence.dao.customer;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.user.Customer;
import com.application.persistence.model.user.VerificationToken;
@Repository
public interface VerificationTokenDAO extends JpaRepository<VerificationToken, Long> {

    VerificationToken findByToken(String token);

    VerificationToken findByUser(Customer user);

    Stream<VerificationToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from VerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
	