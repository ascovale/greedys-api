package com.application.customer.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.post.FcmTokenDTO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.customer.service.CustomerFcmTokenService;
import com.application.customer.service.notification.CustomerNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/customer/notification")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notification", description = "Notification management APIs for customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationController extends BaseController {

    private final CustomerFcmTokenService customerFcmTokenRepository;
    private final CustomerNotificationService notificationService;

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
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Page<CustomerNotification>>> getUnreadNotifications(@PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        return executePaginated("getUnreadNotifications", () -> notificationService.getUnreadNotifications(pageable));
    }

    @Operation(summary = "Get all notifications", description = "Returns a pageable list of all notifications")
    @GetMapping("/all/{page}/{size}")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Page<CustomerNotification>>> getAllNotifications(@PathVariable int page,
            @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        return executePaginated("getAllNotifications", () -> notificationService.getAllNotifications(pageable));
    }

    @Operation(summary = "Set notification as read", description = "Sets the notification with the given ID as the given read boolean")
    @PutMapping("/read")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<String>> setNotificationAsRead(@RequestParam Long notificationId, @RequestParam Boolean read) {
        return executeVoid("setNotificationAsRead", "Notification marked as read", () -> 
            notificationService.read(notificationId));
    }

    @Operation(summary = "Register a user's FCM token", description = "Registers a user's FCM token")
    @PostMapping("/token")
    @CreateApiResponses
    public ResponseEntity<ApiResponse<String>> registerUserFcmToken(@RequestBody FcmTokenDTO userFcmToken) {
        return executeVoid("registerUserFcmToken", "FCM token registered successfully", () -> 
            customerFcmTokenRepository.saveUserFcmToken(userFcmToken));
    }

    @Operation(summary = "Check if a device's token is present", description = "Checks if a device's token is present")
    @GetMapping("/token/present")
    public ResponseEntity<ApiResponse<String>> isDeviceTokenPresent(@RequestParam String deviceId) {
        return execute("isDeviceTokenPresent", () -> {
            boolean isPresent = customerFcmTokenRepository.isDeviceTokenPresent(deviceId);
            if (isPresent) {
                return customerFcmTokenRepository.getTokenByDeviceId(deviceId).getFcmToken();
            }
            return "Token not present";
        });
    }

    @Operation(summary = "Verify a device's token", description = "Verifies a device's token and returns the status")
    @GetMapping("/token/verify")
    public ResponseEntity<ApiResponse<String>> verifyToken(@RequestParam String deviceId) {
        return execute("verifyToken", () -> customerFcmTokenRepository.verifyTokenByDeviceId(deviceId));
    }
}
