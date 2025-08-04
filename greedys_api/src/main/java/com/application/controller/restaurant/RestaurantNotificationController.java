package com.application.controller.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.persistence.model.notification.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.service.RestaurantNotificationService;
import com.application.web.dto.get.notification.RestaurantNotificationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/restaurant/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification Management", description = "Restaurant Notification management APIs")
public class RestaurantNotificationController {
    private final RestaurantNotificationService restaurantNotificationService;

    public RestaurantNotificationController(RestaurantNotificationService restaurantNotificationService) {
        this.restaurantNotificationService = restaurantNotificationService;
    }

    @Operation(summary = "Get unread notifications", description = "Returns a pageable list of unread notifications")
    @GetMapping("/unread/{page}/{size}")
    public ResponseEntity<Page<RestaurantNotificationDTO>> getUnreadNotifications(
            @AuthenticationPrincipal RUser rUser,
            @PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantNotificationDTO> unreadNotifications = restaurantNotificationService.getNotifications(rUser.getId(), pageable, true)
            .map(notification -> RestaurantNotificationDTO.builder()
            .rUserId(rUser.getId())
            .id(notification.getId())
            .title(notification.getTitle())
            .body(notification.getBody())
            .creationTime(notification.getCreationTime())
            .isRead(notification.getIsRead())
            .build());
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
    public ResponseEntity<Page<RestaurantNotificationDTO>> getAllNotifications(
            @AuthenticationPrincipal RUser rUser,
            @PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantNotificationDTO> allNotifications = restaurantNotificationService
            .getNotifications(rUser.getId(), pageable, false)
            .map(notification -> RestaurantNotificationDTO.builder()
                .rUserId(rUser.getId())
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .creationTime(notification.getCreationTime())
                .isRead(notification.getIsRead())
                .build());
        return ResponseEntity.ok().body(allNotifications);
    }

    @Operation(summary = "Get a specific notification", description = "Returns the notification with the given ID")
    @GetMapping("/{notificationId}")
    public ResponseEntity<RestaurantNotificationDTO> getRestaurantNotification(
            @PathVariable Long notificationId) {
        RestaurantNotification notification = restaurantNotificationService.getNotificationById(notificationId);
        RestaurantNotificationDTO dto = RestaurantNotificationDTO.builder()
            .rUserId(notification.getRUser().getId())
            .id(notification.getId())
            .title(notification.getTitle())
            .body(notification.getBody())
            .creationTime(notification.getCreationTime())
            .isRead(notification.getIsRead())
            .build();
        return ResponseEntity.ok().body(dto);
    }

    @Operation(summary = "Set all notifications as read", description = "Sets all notifications for the given user as read")
    @PutMapping("/all-read")
    public ResponseEntity<Void> setAllNotificationsAsRead() {
        restaurantNotificationService.markAllNotificationsAsRead(ControllerUtils.getCurrentRUser().getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get unread notifications count", description = "Returns the count of unread notifications")
    @GetMapping("/unread/count")
    public ResponseEntity<Integer> getUnreadNotificationsCount(@AuthenticationPrincipal RUser rUser) {
        Integer count = restaurantNotificationService.countUnreadNotifications(rUser);
        return ResponseEntity.ok().body(count);
    }
}