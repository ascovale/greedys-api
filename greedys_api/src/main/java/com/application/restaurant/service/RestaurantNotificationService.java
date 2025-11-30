package com.application.restaurant.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.dao.RestaurantNotificationDAO;
import com.application.restaurant.persistence.model.RestaurantNotification;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.service.FirebaseService;
import com.application.common.web.dto.notification.RestaurantNotificationDTO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
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

    private void createAndSendNotifications(Collection<RUser> RUsers, String title, String body) {
        for (RUser RUser : RUsers) {
            RestaurantNotification notification = RestaurantNotification.builder()
                    .title(title)
                    .body(body)
                    .userId(RUser.getId())
                    .read(false)
                    .creationTime(Instant.now())
                    .build();
            RUserDAO.save(RUser);
            restaurantNotificationDAO.save(notification);
            // TODO: Update EmailService to accept new notification model
            // emailService.sendEmailNotification(notification);
        }
    }

    public void handleReservationNotification(Reservation reservation, String title, String body) {
        Restaurant restaurant = reservation.getRestaurant();
        createAndSendNotifications(restaurant.getRUsers(), title, body);
    }

    public Page<RestaurantNotificationDTO> getNotificationsDTO(Long userId, Pageable pageable, boolean unreadOnly) {
        List<RestaurantNotification> notifications = unreadOnly 
            ? restaurantNotificationDAO.findUnreadByUserAndRestaurant(userId, null)
            : restaurantNotificationDAO.findByUserId(userId);
        
        // Paginate manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), notifications.size());
        List<RestaurantNotification> pageContent = notifications.subList(start, end);
        
        return new PageImpl<>(
            pageContent.stream().map(RestaurantNotificationDTO::toDTO).toList(),
            pageable,
            notifications.size()
        );
    }

    public Page<RestaurantNotificationDTO> getNotificationsDTO(Pageable pageable, boolean unreadOnly) {
        List<RestaurantNotification> allNotifications = restaurantNotificationDAO.findAll();
        List<RestaurantNotification> filtered = unreadOnly 
            ? allNotifications.stream().filter(n -> !n.getRead()).toList()
            : allNotifications;
        
        // Paginate manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<RestaurantNotification> pageContent = filtered.subList(start, end);
        
        return new PageImpl<>(
            pageContent.stream().map(RestaurantNotificationDTO::toDTO).toList(),
            pageable,
            filtered.size()
        );
    }

    public RestaurantNotificationDTO updateNotificationReadStatusAndReturnDTO(Long notificationId, Boolean read) {
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(read);
        if (read) {
            notification.setReadAt(Instant.now());
        }
        RestaurantNotification savedNotification = restaurantNotificationDAO.save(notification);
        return RestaurantNotificationDTO.toDTO(savedNotification);
    }

    public void updateNotificationReadStatus(Long notificationId, Boolean read) {
        RestaurantNotification notification = restaurantNotificationDAO.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(read);
        if (read) {
            notification.setReadAt(Instant.now());
        }
        restaurantNotificationDAO.save(notification);
    }

    public Integer countUnreadNotifications(RUser RUser) {
        long unreadCount = restaurantNotificationDAO.countUnreadByUserId(RUser.getId());
        return (int) unreadCount;
    }

    public void markAllNotificationsAsRead(Long idRUser) {
        restaurantNotificationDAO.markAllAsRead(idRUser, Instant.now());
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

    public RestaurantNotificationDTO getNotificationByIdDTO(Long notificationId) {
        RestaurantNotification notification = getNotificationById(notificationId);
        return RestaurantNotificationDTO.toDTO(notification);
    }

    public List<RestaurantNotificationDTO> markAllNotificationsAsReadAndReturn(Long idRUser) {
        // Get all notifications for this user and mark unread ones as read
        List<RestaurantNotification> userNotifications = restaurantNotificationDAO.findByUserId(idRUser)
                .stream()
                .filter(notification -> !notification.getRead())
                .toList();
        
        // Mark them as read
        userNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        });
        List<RestaurantNotification> savedNotifications = restaurantNotificationDAO.saveAll(userNotifications);
        
        // Return DTOs
        return savedNotifications.stream()
                .map(RestaurantNotificationDTO::toDTO)
                .toList();
    }

    /**
     * ⭐ Get count of NEW notifications (arrived SINCE lastMenuOpenedAt)
     * 
     * Definition of "new":
     * - Notifications created AFTER lastMenuOpenedAt timestamp
     * - If lastMenuOpenedAt is NULL, count all notifications
     * 
     * @param rUser The restaurant user
     * @return Count of new notifications since last menu open
     */
    public long getNewNotificationsCount(RUser rUser) {
        if (rUser.getLastMenuOpenedAt() == null) {
            // First time opening menu - return all notifications
            return restaurantNotificationDAO.countByUserId(rUser.getId());
        }
        
        // Count notifications created after lastMenuOpenedAt
        return restaurantNotificationDAO.countByUserIdAndCreatedAfter(rUser.getId(), rUser.getLastMenuOpenedAt());
    }

    /**
     * ⭐ Update lastMenuOpenedAt timestamp
     * 
     * Called when staff opens the notification menu.
     * This resets the "new notifications" counter.
     * 
     * @param rUser The restaurant user
     */
    public void updateLastMenuOpenedAt(RUser rUser) {
        rUser.setLastMenuOpenedAt(Instant.now());
        RUserDAO.save(rUser);
    }
}