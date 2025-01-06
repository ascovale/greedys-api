// FILE: TestDataConfig.java
package com.application.test;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.persistence.model.user.User;
import com.application.service.UserService;
import com.application.web.dto.post.NewUserDTO;

import com.application.mapper.Mapper.Weekday;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class TestDataConfig {

	@Autowired
	private RestaurantDAO restaurantDAO;
	@Autowired
	private ServiceTypeDAO serviceTypeDAO;
	@Autowired
	private ServiceDAO serviceDAO;
	@Autowired
	private SlotDAO slotDAO;
	@Autowired
	private RestaurantUserDAO ruDAO;
	@Autowired
	private UserService userService;
	@Autowired
	private UserDAO userDAO;

	@PostConstruct
	public void init() {
		createRestaurantLaSoffittaRenovatio();
	}

	@Transactional
	private void createRestaurantLaSoffittaRenovatio() {
		Restaurant restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
		if (restaurant == null) {
			restaurant = new Restaurant();
			restaurant.setName("La Soffitta Renovatio");
			restaurant.setAddress("Piazza del Risorgimento 46A");
			restaurant.setPostCode("00192");
			restaurant.setCreationDate(LocalDate.now());
			restaurant.setEmail("info@lasoffittarenovatio.it");
			restaurant = restaurantDAO.save(restaurant);

			ServiceType pranzoType = new ServiceType();
			Service pranzo = new Service();
			pranzoType.setName("Pranzo");

			pranzo.setValidFrom(LocalDate.now());
			pranzo.setValidTo(LocalDate.now());
			pranzo.setRestaurant(restaurant);
			serviceTypeDAO.save(pranzoType);
			serviceDAO.save(pranzo);
			createSlotsForService(pranzo, LocalTime.of(11, 0), LocalTime.of(17, 0));

			ServiceType cenaType = new ServiceType();
			Service cena = new Service();
			cenaType.setName("Cena");

			cena.setValidFrom(LocalDate.now());
			cena.setValidTo(LocalDate.now());
			cena.setRestaurant(restaurant);
			serviceTypeDAO.save(cenaType);
			serviceDAO.save(cena);
			createSlotsForService(cena, LocalTime.of(17, 30), LocalTime.of(23, 0));

			NewUserDTO userDTO = new NewUserDTO();
			userDTO.setFirstName("Stefano");
			userDTO.setLastName("Di Michele");
			userDTO.setPassword("Minosse100%");
			userDTO.setEmail("info@lasoffittarenovatio.it");
			User user= userService.registerNewUserAccount(userDTO);
			user.setEnabled(true);
			userDAO.save(user);

			RestaurantUser ru = new RestaurantUser();
			ru.setRestaurant(restaurant);
			ru.setUser(user);
			ruDAO.save(ru);
		}
	}

	private List<Slot> createSlotsForService(Service service, LocalTime startTime, LocalTime endTime) {
		List<Slot> slots = new ArrayList<>();
		LocalTime time = startTime;
		while (time.isBefore(endTime)) {
			for (int day = 1; day <= 7; day++) {
				Weekday weekday = Weekday.values()[day - 1];
				Slot slot = new Slot(time, time.plusMinutes(30));
				slot.setService(service);
				slot.setWeekday(weekday);
				slots.add(slot);
			}
			time = time.plusMinutes(30);
		}
		// Save slots to the database
		for (Slot slot : slots) {
			slotDAO.save(slot);
		}
		return slots;
	}
}