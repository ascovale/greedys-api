package com.application.restaurant.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.EmailService;
import com.application.common.service.FirebaseService;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RestaurantNotificationDAO;
import com.application.restaurant.persistence.model.RNotificationType;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantNotification;
import com.application.restaurant.persistence.model.user.RUser;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RestaurantNotificationService {
    private final RUserDAO RUserDAO;
    private final RestaurantDAO restaurantDAO;
    private final RestaurantNotificationDAO restaurantNotificationDAO;
    private final FirebaseService firebaseService;
    private final RUserFcmTokenService tokenService;
    private final EmailService emailService;

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

    public Page<RestaurantNotification> getNotifications(Pageable pageable, boolean unreadOnly) {
        return unreadOnly ? restaurantNotificationDAO.findByReadFalse(pageable) : restaurantNotificationDAO.findAll(pageable);
    }

    public void updateNotificationReadStatus(Long notificationId, Boolean read) {
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(read);
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

    public void sendNotificationToAllUsers(String title, String body, Map<String, String> data, Long idRestaurant) {
        Restaurant restaurant = restaurantDAO.findById(idRestaurant)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        createAndSendFirebaseNotifications(restaurant.getRUsers(), title, body, data);
    }

    private void createAndSendFirebaseNotifications(Collection<RUser> RUsers, String title, String body, Map<String, String> data) {
        for (RUser RUser : RUsers) {
            List<String> tokens = tokenService.getTokensByRUserId(RUser.getId());
            firebaseService.sendNotification(title, body, data, tokens);
        }
    }

    public RestaurantNotification getNotificationById(Long notificationId) {
        return restaurantNotificationDAO.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
    }
}
