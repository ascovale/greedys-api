package com.application.restaurant.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.PageResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.persistence.model.RestaurantNotification;
import com.application.restaurant.service.RestaurantNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    public ResponseEntity<PageResponseWrapper<RestaurantNotification>> getUnreadNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        return executePaginated("get unread notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotifications(pageable, true);
        });
    }

    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @ApiResponse(responseCode = "200", description = "Notification status updated successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @ReadApiResponses
    @PutMapping("/read")
    public ResponseEntity<ResponseWrapper<String>> setNotificationAsRead(
            @RequestParam Long notificationId, @RequestParam Boolean read) {
        return executeVoid("set notification as read", "Notification status updated successfully", () -> 
            restaurantNotificationService.updateNotificationReadStatus(notificationId, read));
    }

    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @ReadApiResponses
    @GetMapping("/all/{page}/{size}")
    public ResponseEntity<PageResponseWrapper<RestaurantNotification>> getAllNotifications(
            @PathVariable int page,
            @PathVariable int size) {
        return executePaginated("get all notifications", () -> {
            Pageable pageable = PageRequest.of(page, size);
            return restaurantNotificationService.getNotifications(pageable, false);
        });
    }

    @Operation(summary = "Get a specific notification", description = "Returns the notification with the given ID")
    @ReadApiResponses
    @GetMapping("/{notificationId}")
    public ResponseEntity<ResponseWrapper<RestaurantNotification>> getRestaurantNotification(
            @PathVariable Long notificationId) {
        return execute("get notification", () -> restaurantNotificationService.getNotificationById(notificationId));
    }

    @Operation(summary = "Set all notifications as read", description = "Sets all notifications for the given user as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @ReadApiResponses
    @PutMapping("/all-read")
    public ResponseEntity<ResponseWrapper<String>> setAllNotificationsAsRead() {
        return executeVoid("mark all notifications as read", "All notifications marked as read", () -> 
            restaurantNotificationService.markAllNotificationsAsRead(RestaurantControllerUtils.getCurrentRUser().getId()));
    }

    @Operation(summary = "Get unread notifications count", description = "Returns the count of unread notifications")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully", 
                content = @Content(schema = @Schema(implementation = Long.class)))
    @ReadApiResponses
    @GetMapping("/unread/count")
    public ResponseEntity<ResponseWrapper<Long>> getUnreadNotificationsCount() {
        return execute("get unread notifications count", () -> 
            restaurantNotificationService.countUnreadNotifications(RestaurantControllerUtils.getCurrentRUser()).longValue());
    }
}
