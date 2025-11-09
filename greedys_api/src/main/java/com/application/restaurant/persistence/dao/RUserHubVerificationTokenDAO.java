package com.application.restaurant.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.user.RUserHubVerificationToken;

@Repository
public interface RUserHubVerificationTokenDAO extends JpaRepository<RUserHubVerificationToken, Long> {
    
    RUserHubVerificationToken findByToken(@Param("token") String token);
    
    void deleteByToken(@Param("token") String token);
}