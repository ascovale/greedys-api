package com.application.restaurant.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
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
    @ReadApiResponses
    @GetMapping("/unread/{page}/{size}")
    public ResponseEntity<Page<RestaurantNotificationDTO>> getUnreadNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        return executePaginated("get unread notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotificationsDTO(pageable, true);
        });
    }

    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @ReadApiResponses
    @PutMapping("/read")
    public ResponseEntity<RestaurantNotificationDTO> setNotificationAsRead(
            @RequestParam Long notificationId, @RequestParam Boolean read) {
        return execute("set notification as read", () -> 
            restaurantNotificationService.updateNotificationReadStatusAndReturnDTO(notificationId, read));
    }

    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @ReadApiResponses
    @GetMapping("/all/{page}/{size}")
    public ResponseEntity<Page<RestaurantNotificationDTO>> getAllNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        return executePaginated("get all notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotificationsDTO(pageable, false);
        });
    }

    @Operation(summary = "Get a specific notification", description = "Returns the notification with the given ID")
    @ReadApiResponses
    @GetMapping("/{notificationId}")
    public ResponseEntity<RestaurantNotificationDTO> getRestaurantNotification(
            @PathVariable Long notificationId) {
        return execute("get notification", () -> restaurantNotificationService.getNotificationByIdDTO(notificationId));
    }

    @Operation(summary = "Set all notifications as read", description = "Sets all notifications for the given user as read")
    @PutMapping("/all-read")
    public ResponseEntity<List<RestaurantNotificationDTO>> setAllNotificationsAsRead(@AuthenticationPrincipal RUser rUser) {
        return execute("mark all notifications as read", () -> 
            restaurantNotificationService.markAllNotificationsAsReadAndReturn(rUser.getId()));
    }

    @Operation(summary = "Get unread notifications count", description = "Returns the count of unread notifications")
    
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationsCount(@AuthenticationPrincipal RUser rUser) {
        return execute("get unread notifications count", () -> 
            restaurantNotificationService.countUnreadNotifications(rUser).longValue());
    }

    // ========================================
    // ⭐ NEW ENDPOINTS FOR RESTAURANT NOTIFICATIONS
    // ========================================

    /**
     * ⭐ GET /restaurant/notifications/badge
     * 
     * Returns the count of NEW notifications (arrived SINCE lastMenuOpenedAt).
     * 
     * Definition of "new":
     * - NOT "read vs unread"
     * - It is "arrived SINCE last time notification menu was opened"
     * - Example: Staff opens menu at 14:00 → 3 notifications arrive → badge shows 3
     *           Staff opens menu again at 14:20 → 1 more notification arrives → badge shows 1
     * 
     * @param rUser Authenticated restaurant user
     * @return JSON: { "unreadCount": 3 }
     */
    @Operation(summary = "Get notification badge count", description = "Returns count of NEW notifications (since last menu open)")
    @GetMapping("/badge")
    public ResponseEntity<Map<String, Long>> getNotificationBadge(@AuthenticationPrincipal RUser rUser) {
        return execute("get notification badge", () -> {
            long newCount = restaurantNotificationService.getNewNotificationsCount(rUser);
            Map<String, Long> response = new HashMap<>();
            response.put("unreadCount", newCount);
            return response;
        });
    }

    /**
     * ⭐ POST /restaurant/notifications/menu-open
     * 
     * Called when staff opens the notification menu.
     * Updates lastMenuOpenedAt timestamp to NOW.
     * This resets the badge counter.
     * 
     * @param rUser Authenticated restaurant user
     * @return JSON: { "success": true }
     */
    @Operation(summary = "Mark notification menu as opened", description = "Updates lastMenuOpenedAt timestamp")
    @PostMapping("/menu-open")
    public ResponseEntity<Map<String, Boolean>> markMenuAsOpened(@AuthenticationPrincipal RUser rUser) {
        return execute("mark menu opened", () -> {
            restaurantNotificationService.updateLastMenuOpenedAt(rUser);
            Map<String, Boolean> response = new HashMap<>();
            response.put("success", true);
            return response;
        });
    }

    /**
     * ⭐ GET /restaurant/notifications?page=0&size=20
     * 
     * Lists all notifications (paginated).
     * 
     * @param rUser Authenticated restaurant user
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of NotificationDto
     */
    @Operation(summary = "List all notifications paginated", description = "Returns paginated list of all notifications for the authenticated user")
    @GetMapping("")
    public ResponseEntity<Page<RestaurantNotificationDTO>> listNotifications(
            @AuthenticationPrincipal RUser rUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return executePaginated("list notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotificationsDTO(rUser.getId(), pageable, false);
        });
    }
}

