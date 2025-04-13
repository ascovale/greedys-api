package com.application.persistence.dao.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.menu.Allergen;

@Repository
public interface AllergenDAO extends JpaRepository<Allergen, Long> {
    // Additional query methods if needed
}
