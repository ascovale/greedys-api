package com.application.restaurant.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.restaurant.dao.RestaurantServiceDAO;


@Service("restaurantServizioService")
@Transactional
public class RestaurantServizioService{
	@Autowired
	private RestaurantServiceDAO restaurantServiceDAO;

	public List<com.application.common.persistence.model.reservation.Service> getServices(Long idRestaurant, LocalDate date) {
		return restaurantServiceDAO.getServices(idRestaurant);//, date);
	}
}
