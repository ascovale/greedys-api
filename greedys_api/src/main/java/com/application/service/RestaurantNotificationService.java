package com.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantNotificationDAO;
import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.model.notification.RNotificationType;
import com.application.persistence.model.notification.RestaurantNotification;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RUser;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RestaurantNotificationService {
    private final RUserDAO RUserDAO;
    private final RestaurantDAO restaurantDAO;
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final FirebaseService firebaseService;
    private final RUserFcmTokenService tokenService;
    private final EmailService emailService;

    public RestaurantNotificationService(RUserDAO RUserDAO, RestaurantNotificationDAO restaurantNotificationDAO,
            FirebaseService firebaseService, RUserFcmTokenService tokenService, EmailService emailService, RestaurantDAO restaurantDAO) {
        this.RUserDAO = RUserDAO;
        this.restaurantNotificationDAO = restaurantNotificationDAO;
        this.firebaseService = firebaseService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.restaurantDAO = restaurantDAO;
    }

    private void createAndSendNotifications(Collection<RUser> RUsers, RNotificationType type) {
        for (RUser RUser : RUsers) {
            RestaurantNotification notification = type.create(RUser);
            RUserDAO.save(RUser);
            restaurantNotificationDAO.save(notification);
            emailService.sendEmailNotification(notification);
        }
    }

    public void handleReservationNotification(Reservation reservation, RNotificationType type) {
        Restaurant restaurant = reservation.getRestaurant();
        createAndSendNotifications(restaurant.getRUsers(), type);
    }

    public Page<RestaurantNotification> getNotifications(Long userId, Pageable pageable, boolean unreadOnly) {
        return unreadOnly ? restaurantNotificationDAO.findByUserAndReadPagable(userId, pageable) : restaurantNotificationDAO.findByUserPagable(userId, pageable);
    }

    public void updateNotificationReadStatus(Long notificationId, Boolean read) {
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setIsRead(read);
        restaurantNotificationDAO.save(notification);
    }

    public Integer countUnreadNotifications(RUser RUser) {
        RUser user = RUserDAO.findById(RUser.getId()).orElseThrow(() -> new IllegalArgumentException("RUser not found"));
        return user.getToReadNotification();
    }

    public void markAllNotificationsAsRead(Long idRUser) {
        RUser user = RUserDAO.findById(idRUser).orElseThrow(() -> new IllegalArgumentException("RUser not found"));
        user.setToReadNotification(0);
        RUserDAO.save(user);
    }

    public void sendNotificationToUser(String title, String body, Map<String, String> data, Long idRUser) {
        RUser RUser = RUserDAO.findById(idRUser)
                .orElseThrow(() -> new IllegalArgumentException("RUser not found"));
        List<String> tokens = tokenService.getTokensByRUserId(RUser.getId());
        firebaseService.sendNotification(title, body, data, tokens);
    }

    public void sendNotificationToAllUsers(String title, String body, Map<String, String> data, RNotificationType type, Long idRestaurant) {
        Restaurant restaurant = restaurantDAO.findById(idRestaurant)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        createAndSendFirebaseNotifications(restaurant.getRUsers(), title, body, data, type);
    }

    private void createAndSendFirebaseNotifications(Collection<RUser> RUsers, String title, String body, Map<String, String> data, RNotificationType type) {
        for (RUser RUser : RUsers) {

            RestaurantNotification notification = RestaurantNotification.builder()
                    .title(title)
                    .body(body)
                    .properties(data)
                    .RUser(RUser)
                    .type(type)
                    .build();
            restaurantNotificationDAO.save(notification);
            List<String> tokens = tokenService.getTokensByRUserId(RUser.getId());
            firebaseService.sendNotification(title, body, data, tokens);
        }
    }

    public RestaurantNotification getNotificationById(Long notificationId) {
        return restaurantNotificationDAO.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    }
}
