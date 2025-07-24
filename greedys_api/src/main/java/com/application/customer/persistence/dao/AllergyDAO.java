package com.application.customer.persistence.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.customer.persistence.model.Allergy;


public interface AllergyDAO extends JpaRepository<Allergy,Long>{    
    Optional<Allergy> findByName(String name);
}