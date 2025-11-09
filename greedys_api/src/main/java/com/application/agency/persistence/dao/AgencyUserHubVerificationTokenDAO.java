package com.application.agency.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.agency.persistence.model.user.AgencyUserHubVerificationToken;

@Repository
public interface AgencyUserHubVerificationTokenDAO extends JpaRepository<AgencyUserHubVerificationToken, Long> {
    
    AgencyUserHubVerificationToken findByToken(@Param("token") String token);
    
    void deleteByToken(@Param("token") String token);
}