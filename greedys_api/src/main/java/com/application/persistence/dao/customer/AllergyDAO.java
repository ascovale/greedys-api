package com.application.persistence.dao.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.persistence.model.customer.Allergy;


public interface AllergyDAO extends JpaRepository<Allergy,Long>{
}