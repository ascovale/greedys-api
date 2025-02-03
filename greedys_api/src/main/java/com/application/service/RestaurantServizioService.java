package com.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantServiceDAO;


@Transactional
@Service("restaurantServizioService")
public class RestaurantServizioService{
	@Autowired
	private RestaurantServiceDAO restaurantServiceDAO;

	public List<com.application.persistence.model.reservation.Service> getServices(Long idRestaurant, LocalDate date) {
		return restaurantServiceDAO.getServices(idRestaurant);//, date);
	}
}
