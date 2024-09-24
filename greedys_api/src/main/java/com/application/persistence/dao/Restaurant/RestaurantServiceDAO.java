package com.application.persistence.dao.Restaurant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.model.reservation.Service;


@Transactional(readOnly = true) 
@Repository
public interface RestaurantServiceDAO extends  JpaRepository<Service, Long> {
	@Query(		value = "SELECT * FROM service rs WHERE "
			+ "rs.restaurant_id= :idRestaurant", 
	  nativeQuery = true)
	public List<Service> getServices(@Param("idRestaurant")Long idRestaurant);
}