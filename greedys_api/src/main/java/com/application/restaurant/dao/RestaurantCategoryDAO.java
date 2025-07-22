package com.application.restaurant.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.restaurant.model.RestaurantCategory;

public interface RestaurantCategoryDAO extends JpaRepository<RestaurantCategory, Long>  {

    RestaurantCategory findByName(String categoryName);
    
}
