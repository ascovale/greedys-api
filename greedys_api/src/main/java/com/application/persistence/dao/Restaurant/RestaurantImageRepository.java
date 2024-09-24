package com.application.persistence.dao.Restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.RestaurantImage;
@Repository
public interface RestaurantImageRepository extends JpaRepository<RestaurantImage, Long>{

	RestaurantImage findByName(String text);
	RestaurantImage findFirstByName(String text);

}
