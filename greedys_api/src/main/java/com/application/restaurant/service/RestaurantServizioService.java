package com.application.restaurant.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.dao.RestaurantServiceDAO;
import lombok.RequiredArgsConstructor;

@Service("restaurantServizioService")
@Transactional
@RequiredArgsConstructor
public class RestaurantServizioService{
	
	private final RestaurantServiceDAO restaurantServiceDAO;

	public List<com.application.common.persistence.model.reservation.Service> getServices(Long idRestaurant, LocalDate date) {
		return restaurantServiceDAO.getServices(idRestaurant);//, date);
	}
}
