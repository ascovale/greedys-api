package com.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantNotificationDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RestaurantUser;

import jakarta.transaction.Transactional;

@Service
public class RestaurantNotificationService {
	private final RestaurantUserDAO restaurantUserDAO;
	private final RestaurantDAO restaurantDAO;
	private final RestaurantNotificationDAO restaurantNotificationDAO;
	private final FirebaseService firebaseService;
	private final EmailService emailService;

	public RestaurantNotificationService(RestaurantUserDAO restaurantUserDAO, RestaurantNotificationDAO restaurantNotificationDAO,
			FirebaseService firebaseService, EmailService emailService,RestaurantDAO restaurantDAO) {
		this.restaurantUserDAO = restaurantUserDAO;
		this.restaurantNotificationDAO = restaurantNotificationDAO;
		this.firebaseService = firebaseService;
		this.emailService = emailService;
		this.restaurantDAO = restaurantDAO;
	}

	@Transactional
	public void createNotificationsForRestaurant(Restaurant restaurant, RestaurantNotification.Type type) {
		Collection<RestaurantUser> restaurantUsers = restaurant.getRestaurantUsers();
		for (RestaurantUser restaurantUser : restaurantUsers) {
			RestaurantNotification notification = new RestaurantNotification();
			notification.setRestaurantUser(restaurantUser);
			notification.setType(type);
			notification.setOpened(false);
			restaurantUser.setToReadNotification(restaurantUser.getToReadNotification() + 1);
			restaurantUserDAO.save(restaurantUser);
			restaurantNotificationDAO.save(notification);
			firebaseService.sendFirebaseNotification(notification);
			emailService.sendEmailNotification(notification);
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
			restaurantUser.setToReadNotification(restaurantUser.getToReadNotification() + 1);
			restaurantUserDAO.save(restaurantUser);
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

	public Integer countNotification(RestaurantUser restaurantUser) {
        RestaurantUser user = restaurantUserDAO.findById(restaurantUser.getId()).get();    
        return user.getToReadNotification();
    }

	@Transactional
    public void readNotification(RestaurantUser user) {
        user.setToReadNotification(0);
        restaurantUserDAO.save(user);    
    }

    public RestaurantNotification getRestaurantNotification(Long notificationId) {
	return restaurantNotificationDAO.findById(notificationId)
		.orElseThrow(() -> new IllegalArgumentException("Notification not found"));
	}

    public void setAllNotificationsAsRead(Long idRestaurantUser) {
        RestaurantUser user = restaurantUserDAO.findById(idRestaurantUser).get();
		user.setToReadNotification(0);
		restaurantUserDAO.save(user);
	}

    public Long getUnreadNotificationsCount(Long idRestaurantUser) {
		RestaurantUser user = restaurantUserDAO.findById(idRestaurantUser).get();
		return user.getToReadNotification().longValue();
	}

    public void sendRestaurantNotification(String title, String body, Long idRestaurantUser) {
		firebaseService.sendFirebaseRestaurantNotification(title, body, idRestaurantUser);
	}
	public void sendRestaurantNotificationToAllUsers(String title, String body, Long idRestaurant) {
		Restaurant restaurant = restaurantDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
		Collection<RestaurantUser> restaurantUsers = restaurant.getRestaurantUsers();
		for (RestaurantUser restaurantUser : restaurantUsers) {
			firebaseService.sendFirebaseRestaurantNotification(title, body, restaurantUser.getId());
		}
	}
}
