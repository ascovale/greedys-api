package com.application.controller.admin.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.EmailService;
import com.application.service.RestaurantNotificationService;
import com.application.service.notification.CustomerNotificationService;
import com.application.web.dto.post.admin.EmailRequestDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "adminBearerAuth")
@Tag(name = "Admin Tester", description = "Admin management APIs for testing purposes")
public class AdminTestEmailController {
    //TODO: Dividere le mail per la prenotazione e per la registrazione
    //TODO: Bisognerà usare Twilio per inviare anche le mail per grossi volumi valutare aws
    private EmailService emailService;
    private CustomerNotificationService notificationService;
    private RestaurantNotificationService restaurantNotificationService;

    @Autowired
    public AdminTestEmailController(EmailService emailService, CustomerNotificationService notificationService, RestaurantNotificationService restaurantNotificationService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.restaurantNotificationService = restaurantNotificationService;
    }
    
    @Operation(
        summary = "Send test email", 
        description = "Sends a test email with the specified subject and content",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email request payload",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmailRequestDTO.class))
        )
    )
    @ApiResponse(responseCode = "200", description = "Email sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/send_test_email")
    public GenericResponse sendTestEmail(@RequestBody EmailRequestDTO emailRequest) {
        try {
            System.out.println("\n\n\nTrying to send Test email to: " + emailRequest.getEmail());
            System.out.println("Subject: " + emailRequest.getSubject());
            System.out.println("Message: " + emailRequest.getMessage() + "\n\n\n");
            emailService.sendTestEmail(emailRequest.getEmail(), emailRequest.getSubject(), emailRequest.getMessage());
            return new GenericResponse("Email sent successfully");
        } catch (Exception e) {
            e.printStackTrace(); // Log dettagliato per catturare eventuali errori
            return new GenericResponse("Failed to send email: " + e.getMessage());
        }
    }

    @Operation(summary = "Send test notification to restaurant User", description = "Sends a test notification with the specified title and body to the specified restaurant user")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/send_RUser_test_notification")
    public GenericResponse sendTestNotification(@RequestBody NotificationRequest notificationRequest) {
        
        restaurantNotificationService.sendRestaurantNotification(notificationRequest.getTitle(), notificationRequest.getBody(), notificationRequest.getIdRUser());
        return new GenericResponse("Notification sent successfully");
    }

    @Operation(summary = "Send test user notification", description = "Sends a test notification with the specified title and body to the specified user")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/send_user_test_notification")
    public GenericResponse sendUserTestNotification(@RequestBody NotificationRequest notificationRequest) {
        notificationService.sendNotification(notificationRequest.getTitle(), notificationRequest.getBody(), null, notificationRequest.getIdRUser());
        return new GenericResponse("Notification sent successfully");
    }

    @Operation(summary = "Send test restaurant notification", description = "Sends a test notification with the specified title and body to the restaurant")
    @ApiResponse(responseCode = "200", description = "Notification sent successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/send_test_restaurant_notification")
    public GenericResponse sendTestRestaurantNotification(@RequestBody NotificationRequest notificationRequest) {
        restaurantNotificationService.sendRestaurantNotification(notificationRequest.getTitle(), notificationRequest.getBody(), null);
        return new GenericResponse("Notification sent successfully");
    }
    /**
     * Public method to manually test the SMTP connection.
     * Can be invoked from a test endpoint or startup command.
     */
    @Operation(summary = "Test SMTP connection", description = "Tests the SMTP connection for sending emails")
    @ApiResponse(responseCode = "200", description = "Connection successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "500", description = "Connection failed")
    @PostMapping("/test_smtp_connection")
    public GenericResponse testSmtpConnection() {
        try {
            System.out.println("Testing connection to SMTP server...");
            // Controllo di tipo prima del cast
            Object sender = emailService.getMailSender();
            if (!(sender instanceof JavaMailSenderImpl)) {
                return new GenericResponse("MailSender is not an instance of JavaMailSenderImpl: " + sender.getClass().getName());
            }
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) sender;
            // Imposta timeout di connessione e lettura (es. 15 secondi)
            mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "100000");
            mailSender.getJavaMailProperties().put("mail.smtp.timeout", "100000");
            mailSender.testConnection();
            System.out.println("Connection successful.");
            return new GenericResponse("SMTP connection successful");
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("MessagingException: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("Caused by: " + cause.getMessage());
                cause = cause.getCause();
            }
            e.printStackTrace();
            return new GenericResponse("SMTP connection failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return new GenericResponse("Unexpected error during SMTP connection test: " + e.getMessage());
        }
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
