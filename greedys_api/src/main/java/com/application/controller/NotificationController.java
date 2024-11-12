package com.application.controller;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.application.service.NotificationService;
import com.application.service.ReservationService;
import com.application.service.UserFcmTokenService;
import com.application.service.UserService;
import com.application.web.dto.post.UserFcmTokenDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.user.User;
import com.application.persistence.model.user.UserFcmToken;
import com.google.firebase.auth.FirebaseToken;

@RestController
@RequestMapping("/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserFcmTokenService userFcmTokenService;
    private final ReservationService reservationService;
    private final UserDAO userService;

    public NotificationController(NotificationService notificationService, UserFcmTokenService userFcmTokenService, ReservationService reservationService, UserDAO userService) {
        this.userFcmTokenService = userFcmTokenService;
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

    @Operation(summary = "Register a user's FCM token", description = "Registers a user's FCM token")
    @PostMapping("/token")
    public ResponseEntity<Void> registerUserFcmToken(
            @RequestBody UserFcmTokenDTO userFcmToken) {
        userFcmTokenService.saveUserFcmToken(userFcmToken);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update a user's FCM token", description = "Updates a user's FCM token")
    @PutMapping("/token")
    public ResponseEntity<Void> updateUserFcmToken(
            @RequestParam String oldToken,
            @RequestBody UserFcmTokenDTO newToken) {
        userFcmTokenService.updateUserFcmToken(oldToken, newToken);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Check if a device's token is present", description = "Checks if a device's token is present")
    @GetMapping("/token/present")
    public ResponseEntity<String> isDeviceTokenPresent(
            @RequestParam String deviceId) {
        boolean isPresent = userFcmTokenService.isDeviceTokenPresent(deviceId);
        if (isPresent) {
            Optional<String> oldToken = notificationService.getOldTokenIfPresent(deviceId);
            if (oldToken.isPresent()) {
                return ResponseEntity.ok().body(oldToken.get());
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Token not present");
    }

    @Operation(summary = "Verify a device's token", description = "Verifies a device's token and returns the status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "406", description = "Token expired")
    })
    @GetMapping("/token/verify")
    public ResponseEntity<String> verifyToken(
            @RequestParam String deviceId) {
        String status = userFcmTokenService.verifyTokenByDeviceId(deviceId);
        switch (status) {
            case "OK":
                return ResponseEntity.ok().body("OK");
            case "EXPIRED":
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("EXPIRED");
            case "NOT FOUND":
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND");
            default:
                Optional<String> oldToken = notificationService.getOldTokenIfPresent(deviceId);
                if (oldToken.isPresent()) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR: Old token found - " + oldToken.get());
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
                }
        }
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
