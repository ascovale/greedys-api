package com.application.customer.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.customer.model.Allergy;


public interface AllergyDAO extends JpaRepository<Allergy,Long>{    
    Optional<Allergy> findByName(String name);
}