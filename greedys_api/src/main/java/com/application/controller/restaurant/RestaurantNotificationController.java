package com.application.controller.restaurant;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.restaurant.RestaurantNotification;
import com.application.service.RestaurantNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/restaurant/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Restaurant Notification", description = "Restaurant Notification management APIs")
public class RestaurantNotificationController {

    private final RestaurantNotificationService restaurantNotificationService;

    public RestaurantNotificationController(RestaurantNotificationService restaurantNotificationService) {
        this.restaurantNotificationService = restaurantNotificationService;
    }
    
    @PreAuthorize("@securityService.hasRestaurantUserPermission(#idRestaurantUser)")
    @Operation(summary = "Get unread notifications", description = "Returns a pageable list of unread notifications")
    @GetMapping("/{idRestaurantUser}/unreadnotifications")
    public ResponseEntity<Page<RestaurantNotification>> getUnreadNotifications(@PathVariable Long idRestaurantUser,Pageable pageable) {
        Page<RestaurantNotification> unreadNotifications = restaurantNotificationService.getUnreadNotifications(pageable);
        return ResponseEntity.ok().body(unreadNotifications);
    }

    @PreAuthorize("@securityService.hasRestaurantUserPermission(#idRestaurantUser)")
    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @PutMapping("/{idRestaurantUser}/read")
    public ResponseEntity<Void> setNotificationAsRead(@PathVariable Long idRestaurantUser,@RequestParam Long notificationId,@RequestParam Boolean read) {
        restaurantNotificationService.setNotificationAsRead(notificationId, read);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@securityService.hasRestaurantUserPermission(#idRestaurantUser)")
    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @GetMapping("/{idRestaurantUser}/all")
    public ResponseEntity<Page<RestaurantNotification>> getAllNotifications(@PathVariable Long idRestaurantUser,Pageable pageable) {
        Page<RestaurantNotification> allNotifications = restaurantNotificationService.getAllNotifications(pageable);
        return ResponseEntity.ok().body(allNotifications);
    }
}