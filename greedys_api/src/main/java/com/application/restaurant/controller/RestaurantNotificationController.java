package com.application.restaurant.controller;

import java.util.List;

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

import com.application.common.controller.BaseController;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.notification.RestaurantNotificationDTO;
import com.application.restaurant.persistence.model.user.RUser;
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
public class RestaurantNotificationController extends BaseController {
    private final RestaurantNotificationService restaurantNotificationService;

    @Operation(summary = "Get unread notifications", description = "Returns a pageable list of unread notifications")
    @GetMapping("/unread/{page}/{size}")
    public ResponseEntity<ResponseWrapper<List<RestaurantNotificationDTO>>> getUnreadNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        return executePaginated("get unread notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotificationsDTO(pageable, true);
        });
    }

    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @PutMapping("/read")
    public ResponseEntity<ResponseWrapper<RestaurantNotificationDTO>> setNotificationAsRead(
            @RequestParam Long notificationId, @RequestParam Boolean read) {
        return execute("set notification as read", () -> 
            restaurantNotificationService.updateNotificationReadStatusAndReturnDTO(notificationId, read));
    }

    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @GetMapping("/all/{page}/{size}")
    public ResponseEntity<ResponseWrapper<List<RestaurantNotificationDTO>>> getAllNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        return executePaginated("get all notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotificationsDTO(pageable, false);
        });
    }

    @Operation(summary = "Get a specific notification", description = "Returns the notification with the given ID")
    @GetMapping("/{notificationId}")
    public ResponseEntity<ResponseWrapper<RestaurantNotificationDTO>> getRestaurantNotification(
            @PathVariable Long notificationId) {
        return execute("get notification", () -> restaurantNotificationService.getNotificationByIdDTO(notificationId));
    }

    @Operation(summary = "Set all notifications as read", description = "Sets all notifications for the given user as read")
    @PutMapping("/all-read")
    public ResponseEntity<ResponseWrapper<List<RestaurantNotificationDTO>>> setAllNotificationsAsRead(@AuthenticationPrincipal RUser rUser) {
        return execute("mark all notifications as read", () -> 
            restaurantNotificationService.markAllNotificationsAsReadAndReturn(rUser.getId()));
    }

    @Operation(summary = "Get unread notifications count", description = "Returns the count of unread notifications")
    
    @GetMapping("/unread/count")
    public ResponseEntity<ResponseWrapper<Long>> getUnreadNotificationsCount(@AuthenticationPrincipal RUser rUser) {
        return execute("get unread notifications count", () -> 
            restaurantNotificationService.countUnreadNotifications(rUser).longValue());
    }
}
