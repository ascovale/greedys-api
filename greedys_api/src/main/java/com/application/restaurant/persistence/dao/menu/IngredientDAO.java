package com.application.restaurant.persistence.dao.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.menu.Ingredient;

@Repository
public interface IngredientDAO extends JpaRepository<Ingredient, Long> {
    // Additional query methods if needed
}
