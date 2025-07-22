package com.application.restaurant.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.utils.ControllerUtils;
import com.application.restaurant.model.RestaurantNotification;
import com.application.restaurant.service.RestaurantNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/restaurant/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification Management", description = "Restaurant Notification management APIs")
@RequiredArgsConstructor
@Slf4j
public class RestaurantNotificationController {
    private final RestaurantNotificationService restaurantNotificationService;

    @Operation(summary = "Get unread notifications", description = "Returns a pageable list of unread notifications")
    @GetMapping("/unread/{page}/{size}")
    public ResponseEntity<Page<RestaurantNotification>> getUnreadNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantNotification> unreadNotifications = restaurantNotificationService.getNotifications(pageable, true);
        return ResponseEntity.ok().body(unreadNotifications);
    }

    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @PutMapping("/read")
    public ResponseEntity<Void> setNotificationAsRead(
            @RequestParam Long notificationId, @RequestParam Boolean read) {
        restaurantNotificationService.updateNotificationReadStatus(notificationId, read);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @GetMapping("/all/{page}/{size}")
    public ResponseEntity<Page<RestaurantNotification>> getAllNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantNotification> allNotifications = restaurantNotificationService.getNotifications(pageable, false);
        return ResponseEntity.ok().body(allNotifications);
    }

    @Operation(summary = "Get a specific notification", description = "Returns the notification with the given ID")
    @GetMapping("/{notificationId}")
    public ResponseEntity<RestaurantNotification> getRestaurantNotification(
            @PathVariable Long notificationId) {
        RestaurantNotification notification = restaurantNotificationService.getNotificationById(notificationId);
        return ResponseEntity.ok().body(notification);
    }

    @Operation(summary = "Set all notifications as read", description = "Sets all notifications for the given user as read")
    @PutMapping("/all-read")
    public ResponseEntity<Void> setAllNotificationsAsRead() {
        restaurantNotificationService.markAllNotificationsAsRead(ControllerUtils.getCurrentRUser().getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get unread notifications count", description = "Returns the count of unread notifications")
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationsCount() {
        Long count = restaurantNotificationService.countUnreadNotifications(ControllerUtils.getCurrentRUser()).longValue();
        return ResponseEntity.ok().body(count);
    }
}