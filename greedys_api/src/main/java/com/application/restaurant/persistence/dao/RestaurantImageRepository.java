package com.application.restaurant.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.RestaurantImage;
@Repository
public interface RestaurantImageRepository extends JpaRepository<RestaurantImage, Long>{

	RestaurantImage findByName(String text);
	RestaurantImage findFirstByName(String text);

}
