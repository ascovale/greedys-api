package com.application.restaurant.dao.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.menu.DishCategory;

@Repository
public interface DishCategoryDAO extends JpaRepository<DishCategory, Long> {
    // Additional query methods if needed
}
