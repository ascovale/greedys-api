package com.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.RestaurantNotificationDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantNotification;
import com.application.persistence.model.restaurant.RestaurantUser;

import jakarta.transaction.Transactional;

@Service
public class RestaurantNotificationService {
	private final RestaurantNotificationDAO restaurantNotificationDAO;
	private final FirebaseService firebaseService;

	public RestaurantNotificationService(RestaurantNotificationDAO restaurantNotificationDAO,
			FirebaseService firebaseService) {
		this.restaurantNotificationDAO = restaurantNotificationDAO;
		this.firebaseService = firebaseService;
	}

	@Transactional
	public void createNotificationsForRestaurant(Restaurant restaurant, RestaurantNotification.Type type) {
		Collection<RestaurantUser> restaurantUsers = restaurant.getRestaurantUsers();
		for (RestaurantUser restaurantUser : restaurantUsers) {
			RestaurantNotification notification = new RestaurantNotification();
			notification.setRestaurantUser(restaurantUser);
			notification.setType(type);
			notification.setOpened(false);
			restaurantNotificationDAO.save(notification);
			firebaseService.sendFirebaseNotification(notification);
		}
	}

	public List<RestaurantNotification> newReservationNotification(Reservation reservation) {
		List<RestaurantNotification> notifications = new ArrayList<>();
		Restaurant restaurant = reservation.getRestaurant();
		Collection<RestaurantUser> restaurantUsers = restaurant.getRestaurantUsers();
		for (RestaurantUser restaurantUser : restaurantUsers) {
			RestaurantNotification notification = new RestaurantNotification();
			notification.setRestaurantUser(restaurantUser);
			notification.setType(RestaurantNotification.Type.REQUEST);
			notification.setOpened(false);
			restaurantNotificationDAO.save(notification);
			notifications.add(notification);
			firebaseService.sendFirebaseNotification(notification);
		}
		return notifications;
	}

	public void modifyReservationNotification(Reservation reservation) {
		Restaurant restaurant = reservation.getRestaurant();
		createNotificationsForRestaurant(restaurant, RestaurantNotification.Type.REQUEST);
	}

	public void deleteReservationNotification(Reservation reservation) {
		Restaurant restaurant = reservation.getRestaurant();
		createNotificationsForRestaurant(restaurant, RestaurantNotification.Type.REQUEST);
	}

}
