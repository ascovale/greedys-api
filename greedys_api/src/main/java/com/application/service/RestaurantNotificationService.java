package com.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantNotificationDAO;
import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RUser;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RestaurantNotificationService {
	private final RUserDAO RUserDAO;
	private final RestaurantDAO restaurantDAO;
	private final RestaurantNotificationDAO restaurantNotificationDAO;
	private final RUserFirebaseService firebaseService;
	private final EmailService emailService;

	public RestaurantNotificationService(RUserDAO RUserDAO, RestaurantNotificationDAO restaurantNotificationDAO,
			RUserFirebaseService firebaseService, EmailService emailService,RestaurantDAO restaurantDAO) {
		this.RUserDAO = RUserDAO;
		this.restaurantNotificationDAO = restaurantNotificationDAO;
		this.firebaseService = firebaseService;
		this.emailService = emailService;
		this.restaurantDAO = restaurantDAO;
	}

	@Transactional
	public void createNotificationsForRestaurant(Restaurant restaurant, RestaurantNotification.Type type) {
		Collection<RUser> RUsers = restaurant.getRUsers();
		for (RUser RUser : RUsers) {
			RestaurantNotification notification = new RestaurantNotification();
			notification.setRUser(RUser);
			notification.setType(type);
			notification.setOpened(false);
			RUser.setToReadNotification(RUser.getToReadNotification() + 1);
			RUserDAO.save(RUser);
			restaurantNotificationDAO.save(notification);
			//firebaseService.sendFirebaseNotification(notification);
			emailService.sendEmailNotification(notification);
		}
	}

	public List<RestaurantNotification> newReservationNotification(Reservation reservation) {
		List<RestaurantNotification> notifications = new ArrayList<>();
		Restaurant restaurant = reservation.getRestaurant();
		Collection<RUser> RUsers = restaurant.getRUsers();
		for (RUser RUser : RUsers) {
			RestaurantNotification notification = new RestaurantNotification();
			notification.setRUser(RUser);
			notification.setType(RestaurantNotification.Type.REQUEST);
			notification.setOpened(false);
			RUser.setToReadNotification(RUser.getToReadNotification() + 1);
			RUserDAO.save(RUser);
			restaurantNotificationDAO.save(notification);
			Hibernate.initialize(notification.getRUser());
			notifications.add(notification);
			//firebaseService.sendFirebaseNotification(notification);
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

	public Page<RestaurantNotification> getUnreadNotifications(Pageable pageable) {
		return restaurantNotificationDAO.findByOpenedFalse(pageable);

	}

	public void setNotificationAsRead(Long notificationId, Boolean read) {
		RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
				.orElseThrow(() -> new IllegalArgumentException("Notification not found"));
		notification.setOpened(read);
		restaurantNotificationDAO.save(notification);

	}

	public Page<RestaurantNotification> getAllNotifications(Pageable pageable) {
		return restaurantNotificationDAO.findAll(pageable);
	}

	public Integer countNotification(RUser RUser) {
        RUser user = RUserDAO.findById(RUser.getId()).get();    
        return user.getToReadNotification();
    }

	@Transactional
    public void readNotification(RUser user) {
        user.setToReadNotification(0);
        RUserDAO.save(user);    
    }

    public RestaurantNotification getRestaurantNotification(Long notificationId) {
	return restaurantNotificationDAO.findById(notificationId)
		.orElseThrow(() -> new IllegalArgumentException("Notification not found"));
	}

    public void setAllNotificationsAsRead(Long idRUser) {
        RUser user = RUserDAO.findById(idRUser).get();
		user.setToReadNotification(0);
		RUserDAO.save(user);
	}

    public Long getUnreadNotificationsCount(Long idRUser) {
		RUser user = RUserDAO.findById(idRUser).get();
		return user.getToReadNotification().longValue();
	}

    public void sendRestaurantNotification(String title, String body, Long idRUser) {
		//firebaseService.sendFirebaseRestaurantNotification(title, body, idRUser);
	}
	public void sendRestaurantNotificationToAllUsers(String title, String body, Long idRestaurant) {
		Restaurant restaurant = restaurantDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
		Collection<RUser> RUsers = restaurant.getRUsers();
		for (RUser RUser : RUsers) {
			//firebaseService.sendFirebaseRestaurantNotification(title, body, RUser.getId());
		}
	}
}
