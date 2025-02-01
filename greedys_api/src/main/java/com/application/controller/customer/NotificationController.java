package com.application.controller.customer;


import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.FirebaseService;
import com.application.service.UserFcmTokenService;
import com.application.web.dto.post.UserFcmTokenDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@RequestMapping("/user/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Notification management APIs")
public class NotificationController {

    private final UserFcmTokenService userFcmTokenService;
    private final FirebaseService firebaseService;
    public NotificationController(UserFcmTokenService userFcmTokenService, FirebaseService firebaseService) {
        this.userFcmTokenService = userFcmTokenService;
        this.firebaseService = firebaseService;
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
            Optional<String> oldToken = firebaseService.getOldTokenIfPresent(deviceId);
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
                Optional<String> oldToken = firebaseService.getOldTokenIfPresent(deviceId);
                if (oldToken.isPresent()) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR: Old token found - " + oldToken.get());
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
                }
        }
    }


}
