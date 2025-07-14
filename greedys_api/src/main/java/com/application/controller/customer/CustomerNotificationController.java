package com.application.controller.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.notification.CustomerNotification;
import com.application.service.CustomerFcmTokenService;
import com.application.service.FirebaseService;
import com.application.service.notification.CustomerNotificationService;
import com.application.web.dto.post.FcmTokenDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/customer/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Notification management APIs for customers")
public class CustomerNotificationController {

    private final CustomerFcmTokenService customerFcmTokenRepository;
    private final CustomerNotificationService notificationService;

    public CustomerNotificationController(CustomerFcmTokenService customerFcmTokenRepository, FirebaseService firebaseService,
            CustomerNotificationService notificationService) {
        this.customerFcmTokenRepository = customerFcmTokenRepository;
        this.notificationService = notificationService;
    }

    /*
     * @Operation(summary = "Get notifications for index page", description =
     * "Returns notifications for the index page")
     * 
     * @GetMapping("/index")
     * public ResponseEntity<String> getNotifications() {
     * return ResponseEntity.ok().body("Index page");
     * }
     * 
     * @Operation(summary = "Get notifications for homepage", description =
     * "Returns notifications for the homepage")
     * 
     * @GetMapping("/homepage")
     * public ResponseEntity<String> getHomepageNotifications() {
     * return ResponseEntity.ok().body("Homepage");
     * }
     * 
     * @Operation(summary = "Get notification list", description =
     * "Returns the list of notifications")
     * 
     * @GetMapping("/list")
     * public ResponseEntity<String> getNotificationList() {
     * return ResponseEntity.ok().body("Notification list page");
     * }
     */

    @Operation(summary = "Get unread notifications", description = "Returns a pageable list of unread notifications")
    @GetMapping("/unread/{page}/{size}")
    public ResponseEntity<Page<CustomerNotification>> getUnreadNotifications(@PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerNotification> unreadNotifications = notificationService.getUnreadNotifications(pageable);
        return ResponseEntity.ok().body(unreadNotifications);
    }

    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @GetMapping("/all/{page}/{size}")
    public ResponseEntity<Page<CustomerNotification>> getAllNotifications(@PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerNotification> allNotifications = notificationService.getAllNotifications(pageable);
        return ResponseEntity.ok().body(allNotifications);
    }

    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @PutMapping("/read")
    public ResponseEntity<Void> setNotificationAsRead(@RequestParam Long notificationId, @RequestParam Boolean read) {
        notificationService.read(notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Register a user's FCM token", description = "Registers a user's FCM token")
    @PostMapping("/token")
    public ResponseEntity<Void> registerUserFcmToken(
            @RequestBody FcmTokenDTO userFcmToken) {
        customerFcmTokenRepository.saveUserFcmToken(userFcmToken);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Check if a device's token is present", description = "Checks if a device's token is present")
    @GetMapping("/token/present")
    public ResponseEntity<String> isDeviceTokenPresent(
            @RequestParam String deviceId) {
        boolean isPresent = customerFcmTokenRepository.isDeviceTokenPresent(deviceId);
        if (isPresent) {
            return ResponseEntity.ok().body(customerFcmTokenRepository.getTokenByDeviceId(deviceId).getFcmToken());
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
        String status = customerFcmTokenRepository.verifyTokenByDeviceId(deviceId);
        switch (status) {
            case "OK":
                return ResponseEntity.ok().body("OK");
            case "EXPIRED":
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("EXPIRED");
            case "NOT FOUND":
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND");
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
        }
    }

}
