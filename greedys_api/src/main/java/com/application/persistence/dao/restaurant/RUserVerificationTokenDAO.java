package com.application.persistence.dao.restaurant;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RUser;
import com.application.persistence.model.restaurant.user.RUserVerificationToken;
@Repository
public interface RUserVerificationTokenDAO extends JpaRepository<RUserVerificationToken, Long> {

    RUserVerificationToken findByToken(String token);

    RUserVerificationToken findByRUser(RUser RUser);
    
    Stream<RUserVerificationToken> findAllByExpiryDateLessThan(LocalDate now);

    void deleteByExpiryDateLessThan(LocalDate now);

    @Modifying
    @Query("delete from RUserVerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDate now);
}
	