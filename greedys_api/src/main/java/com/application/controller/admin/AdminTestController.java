package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.EmailService;
import com.application.service.NotificationService;
import com.application.service.RestaurantNotificationService;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Tester", description = "Admin management APIs for testing purposes")
public class AdminTestController {
    private EmailService emailService;
    private NotificationService notificationService;
    private RestaurantNotificationService restaurantNotificationService;

    @Autowired
    public AdminTestController(EmailService emailService, NotificationService notificationService, RestaurantNotificationService restaurantNotificationService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.restaurantNotificationService = restaurantNotificationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send test email", description = "Sends a test email with the specified subject and content")
    @ApiResponse(responseCode = "200", description = "Email sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/sendTestEmail")
    public GenericResponse sendTestEmail(@RequestBody EmailRequest emailRequest) {
        emailService.sendEmail(emailRequest.getEmail(), emailRequest.getSubject(), emailRequest.getContent());
        return new GenericResponse("Email sent successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send test notification to restaurant User", description = "Sends a test notification with the specified title and body to the specified restaurant user")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/sendRestaurantUserTestNotification")
    public GenericResponse sendTestNotification(@RequestBody NotificationRequest notificationRequest) {
        
        restaurantNotificationService.sendRestaurantNotification(notificationRequest.getTitle(), notificationRequest.getBody(), notificationRequest.getIdRestaurantUser());
        return new GenericResponse("Notification sent successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send test user notification", description = "Sends a test notification with the specified title and body to the specified user")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/sendUserTestNotification")
    public GenericResponse sendUserTestNotification(@RequestBody NotificationRequest notificationRequest) {
        notificationService.sendCustomerNotification(notificationRequest.getTitle(), notificationRequest.getBody(), notificationRequest.getIdRestaurantUser());
        return new GenericResponse("Notification sent successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send test restaurant notification", description = "Sends a test notification with the specified title and body to the restaurant")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/sendTestRestaurantNotification")
    public GenericResponse sendTestRestaurantNotification(@RequestBody NotificationRequest notificationRequest) {
        restaurantNotificationService.sendRestaurantNotification(notificationRequest.getTitle(), notificationRequest.getBody(), null);
        return new GenericResponse("Notification sent successfully");
    }

    public static class NotificationRequest {
        private String title;
        private String body;
        private Long idRestaurantUser;

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Long getIdRestaurantUser() {
            return idRestaurantUser;
        }

        public void setIdRestaurantUser(Long idRestaurantUser) {
            this.idRestaurantUser = idRestaurantUser;
        }
    }

    public static class EmailRequest {
        private String email;
        private String subject;
        private String content;

        // Getters and setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
