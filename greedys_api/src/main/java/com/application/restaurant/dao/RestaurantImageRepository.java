package com.application.restaurant.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.model.RestaurantImage;
@Repository
public interface RestaurantImageRepository extends JpaRepository<RestaurantImage, Long>{

	RestaurantImage findByName(String text);
	RestaurantImage findFirstByName(String text);

}
