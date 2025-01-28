package com.application.persistence.dao.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.persistence.model.restaurant.RestaurantCategory;

public interface RestaurantCategoryDAO extends JpaRepository<RestaurantCategory, Long>  {
    
}
