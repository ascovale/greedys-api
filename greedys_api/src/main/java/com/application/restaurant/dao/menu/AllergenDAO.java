package com.application.restaurant.dao.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.menu.Allergen;

@Repository
public interface AllergenDAO extends JpaRepository<Allergen, Long> {
    // Additional query methods if needed
}
