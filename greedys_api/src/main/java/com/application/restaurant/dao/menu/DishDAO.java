package com.application.restaurant.dao.menu;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.menu.Dish;

@Repository
public interface DishDAO extends JpaRepository<Dish, Long> {
    // Additional query methods if needed
    List<Dish> findByRestaurantId(Long restaurantId);
}
