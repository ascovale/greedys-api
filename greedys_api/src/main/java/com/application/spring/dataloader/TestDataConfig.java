// FILE: TestDataConfig.java
package com.application.spring.dataloader;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.admin.AdminDAO;
import com.application.persistence.dao.admin.AdminRoleDAO;
import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminRole;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Role;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantRole;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.CustomerService;
import com.application.web.dto.post.NewCustomerDTO;

import jakarta.transaction.Transactional;

@Configuration
@DependsOn("setupDataLoader") // Indica che TestDataConfig dipende da SetupDataLoader
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
	private CustomerService userService;
	@Autowired
	private CustomerDAO userDAO;
	@Autowired
	RestaurantRoleDAO restaurantRoleDAO;
	@Autowired
	private AdminDAO adminDAO;
	@Autowired
	private AdminRoleDAO adminRoleDAO;
	@Autowired
	private RoleDAO roleDAO;

	@PostConstruct
	public void init() {
		createSomeAdmin();
		createSomeCustomer();
		createRestaurantLaSoffittaRenovatio();
	}

	@Transactional
	private void createSomeCustomer() {
		Role premiumRole = roleDAO.findByName("ROLE_PREMIUM_USER");

		NewCustomerDTO userDTO = new NewCustomerDTO();
		userDTO.setFirstName("Stefano");
		userDTO.setLastName("Di Michele");
		userDTO.setPassword("Minosse100%");
		userDTO.setEmail("info@lasoffittarenovatio.it");
		Customer user = userService.registerNewUserAccount(userDTO);
		user.setStatus(Customer.Status.ENABLED);
		if (premiumRole != null) {
			user.addRole(premiumRole);
		}
		userDAO.save(user);
	}

	@Transactional
	private void createSomeAdmin() {
		AdminRole adminRole = adminRoleDAO.findByName("ROLE_SUPER_ADMIN");
		Admin admin1 = new Admin();
		admin1.setEmail("ascolesevalentino@gmail.com");
		admin1.setName("Valentino");
		admin1.setSurname("Ascolese");
		admin1.setPassword("Minosse100%");
		admin1.setStatus(Admin.Status.ENABLED);
		admin1.addAdminRole(adminRole);
		adminDAO.save(admin1);

		Admin admin2 = new Admin();
		admin2.setEmail("matteo.rossi1902@gmail.com");
		admin2.setName("Matteo");
		admin2.setSurname("Rossi");
		admin2.setPassword("Minosse100%");
		admin2.setStatus(Admin.Status.ENABLED);
		admin2.addAdminRole(adminRole);
		adminDAO.save(admin2);
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

			RestaurantUser ru = new RestaurantUser();
			ru.setRestaurant(restaurant);
			ru.setEmail("ascolesevalentino@gmail.com");
			ru.setLastName("Ascolese");
			ru.setPassword("Minosse100%");
			ru.setStatus(RestaurantUser.Status.ENABLED);
			ruDAO.save(ru);
			RestaurantRole ownerRole = restaurantRoleDAO.findByName("ROLE_OWNER");
			ru.addRestaurantRole(ownerRole);
			RestaurantUser stefanoUser = new RestaurantUser();
			stefanoUser.setRestaurant(restaurant);
			stefanoUser.setEmail("stefano.dimichele@example.com");
			stefanoUser.setFirstName("Stefano");
			stefanoUser.setLastName("Di Michele");
			stefanoUser.setPassword("Minosse100%");
			stefanoUser.setStatus(RestaurantUser.Status.ENABLED);
			ruDAO.save(stefanoUser);

			RestaurantUser massimoUser = new RestaurantUser();
			massimoUser.setRestaurant(restaurant);
			massimoUser.setEmail("massimo.dimichele@example.com");
			massimoUser.setFirstName("Massimo");
			massimoUser.setLastName("Di Michele");
			massimoUser.setPassword("Minosse100%");
			massimoUser.setStatus(RestaurantUser.Status.ENABLED);
			ruDAO.save(massimoUser);
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