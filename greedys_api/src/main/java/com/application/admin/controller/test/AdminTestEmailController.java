package com.application.admin.controller.test;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.web.dto.post.EmailRequestDTO;
import com.application.common.controller.BaseController;
import com.application.common.service.EmailService;
import com.application.common.web.ApiResponse;
import com.application.customer.service.notification.CustomerNotificationService;
import com.application.restaurant.service.RestaurantNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Tester", description = "Admin management APIs for testing purposes")
@RequiredArgsConstructor
@Slf4j
public class AdminTestEmailController extends BaseController {
    private final EmailService emailService;
    private final CustomerNotificationService notificationService;
    private final RestaurantNotificationService restaurantNotificationService;
    
    @Operation(
        summary = "Send test email", 
        description = "Sends a test email with the specified subject and content",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email request payload",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmailRequestDTO.class))
        )
    )
    @PostMapping("/send_test_email")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(@RequestBody EmailRequestDTO emailRequest) {
        return executeCreate("send test email", "Email sent successfully", () -> {
            log.info("Trying to send Test email to: {}", emailRequest.getEmail());
            log.info("Subject: {}", emailRequest.getSubject());
            log.info("Message: {}", emailRequest.getMessage());
            emailService.sendTestEmail(emailRequest.getEmail(), emailRequest.getSubject(), emailRequest.getMessage());
            return "Email sent successfully";
        });
    }

    @Operation(summary = "Send test notification to restaurant User", description = "Sends a test notification with the specified title and body to the specified restaurant user")
    @PostMapping("/send_RUser_test_notification")
    public ResponseEntity<ApiResponse<String>> sendTestNotification(@RequestBody NotificationRequest notificationRequest) {
        return executeVoid("send test notification", "Notification sent successfully", () -> {
            //restaurantNotificationService.sendRestaurantNotification(notificationRequest.getTitle(), notificationRequest.getBody(), notificationRequest.getIdRUser());
        });
    }

    @Operation(summary = "Send test user notification", description = "Sends a test notification with the specified title and body to the specified user")
    @PostMapping("/send_user_test_notification")
    public ResponseEntity<ApiResponse<String>> sendUserTestNotification(@RequestBody NotificationRequest notificationRequest) {
        return executeVoid("send user test notification", "Notification sent successfully", () -> {
            notificationService.sendNotification(notificationRequest.getTitle(), notificationRequest.getBody(), null, notificationRequest.getIdRUser());
        });
    }

    @Operation(summary = "Send test restaurant notification", description = "Sends a test notification with the specified title and body to the restaurant")
    @PostMapping("/send_test_restaurant_notification")
    public ResponseEntity<ApiResponse<String>> sendTestRestaurantNotification(@RequestBody NotificationRequest notificationRequest) {
        return executeVoid("send test restaurant notification", "Notification sent successfully", () -> {
            //restaurantNotificationService.sendRestaurantNotification(notificationRequest.getTitle(), notificationRequest.getBody(), null);
        });
    }

    /**
     * Public method to manually test the SMTP connection.
     * Can be invoked from a test endpoint or startup command.
     */
    @Operation(summary = "Test SMTP connection", description = "Tests the SMTP connection for sending emails")
    @PostMapping("/test_smtp_connection")
    public ResponseEntity<ApiResponse<String>> testSmtpConnection() {
        return executeCreate("test smtp connection", () -> {
            log.info("Testing connection to SMTP server...");
            Object sender = emailService.getMailSender();
            if (!(sender instanceof JavaMailSenderImpl)) {
                return "MailSender is not an instance of JavaMailSenderImpl: " + sender.getClass().getName();
            }
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) sender;
            mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "100000");
            mailSender.getJavaMailProperties().put("mail.smtp.timeout", "100000");
            mailSender.testConnection();
            log.info("Connection successful.");
            return "SMTP connection successful";
        });
    }
    public static class NotificationRequest {
        private String title;
        private String body;
        private Long idRUser;

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

        public Long getIdRUser() {
            return idRUser;
        }

        public void setIdRUser(Long idRUser) {
            this.idRUser = idRUser;
        }
    }
}
