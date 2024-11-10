package com.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.application.service.NotificationService;
import com.application.service.ReservationService;
import com.application.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.user.User;

@RestController
@RequestMapping("/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;
    private final ReservationService reservationService;
    private final UserDAO userService;

    public NotificationController(NotificationService notificationService, ReservationService reservationService, UserDAO userService) {
        this.notificationService = notificationService;
        this.reservationService = reservationService;
        this.userService = userService;
    }
    
    @Operation(summary = "Get notifications for index page", description = "Returns notifications for the index page")
    @GetMapping("/index")
    public ResponseEntity<String> getNotifications() {
        return ResponseEntity.ok().body("Index page");
    }

    @Operation(summary = "Get notifications for homepage", description = "Returns notifications for the homepage")
    @GetMapping("/homepage")
    public ResponseEntity<String> getHomepageNotifications() {
        return ResponseEntity.ok().body("Homepage");
    }

    @Operation(summary = "Get notification list", description = "Returns the list of notifications")
    @GetMapping("/list")
    public ResponseEntity<String> getNotificationList() {
        return ResponseEntity.ok().body("Notification list page");
    }

    @Operation(summary = "Get notifications for notification4 page", description = "Returns notifications for the notification4 page")
    @GetMapping("/page4")
    public ResponseEntity<String> getNotifications4() {
        return ResponseEntity.ok().body("Notification 4 page");
    }

    @Operation(summary = "Get notifications for notification2 page", description = "Returns notifications for the notification2 page")
    @GetMapping("/page2")
    public ResponseEntity<String> getNotifications2() {
        return ResponseEntity.ok().body("Notification 2 page");
    }

    @Operation(summary = "Send a generic notification", description = "Sends a generic notification to the user")
    @PostMapping("/send")
    public ResponseEntity<Void> sendGenericNotification(
            @Parameter(description = "ID of the user") @RequestParam Long idUser,
            @Parameter(description = "Notification title") @RequestParam String title,
            @Parameter(description = "Notification message") @RequestParam String message) {
        User user = userService.findById(idUser).orElse(null);
        if (user != null) {
            notificationService.sendFirebaseNotification(user, title, message);
        }
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }
}
