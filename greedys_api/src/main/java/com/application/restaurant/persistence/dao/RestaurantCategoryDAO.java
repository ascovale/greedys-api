package com.application.restaurant.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.restaurant.persistence.model.RestaurantCategory;

public interface RestaurantCategoryDAO extends JpaRepository<RestaurantCategory, Long>  {

    RestaurantCategory findByName(String categoryName);
    
}
