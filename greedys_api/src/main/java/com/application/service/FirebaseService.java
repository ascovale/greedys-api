package com.application.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.CustomerFcmToken;
import com.application.persistence.model.customer.Notification;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.utils.NotificatioUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.gson.Gson;

@Service
public class FirebaseService {
    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";
    private static final String FIREBASE_API_URL = "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send";
    private GoogleCredentials googleCredentials;
    private final CustomerFcmTokenService customerFcmTokenService;
    private RestaurantUserDAO restaurantUserDAO;

    public FirebaseService(CustomerFcmTokenService customerFcmTokenService,RestaurantUserDAO restaurantUserDAO)
     {
        this.restaurantUserDAO = restaurantUserDAO;
        this.customerFcmTokenService = customerFcmTokenService;
        try {
            this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream("/run/secrets/service_account"))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            this.googleCredentials.refreshIfExpired();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google Credentials: " + e.getMessage(), e);
        }
    }

    public FirebaseToken verifyToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            // Handle token verification error
            return null;
        }
    }

    @Async
    public void sendFirebaseNotification(Notification notification) {
        Customer user = notification.getCustomer();
        List<CustomerFcmToken> tokens = customerFcmTokenService.getTokensByCustomerId(user.getId());
        for (CustomerFcmToken token : tokens) {
            try {
                googleCredentials.refreshIfExpired();
                String accessToken = googleCredentials.getAccessToken().getTokenValue();
                RestTemplate restTemplate = new RestTemplate();
                // Set HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + accessToken);
                // Create notification body
                Map<String, Object> messageBody = new HashMap<>();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("token", token.getFcmToken());
                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("title", NotificatioUtils.getUserTemplates().get(notification.getType()).getTitle());
                notificationData.put("body", NotificatioUtils.getUserTemplates().get(notification.getType()).getMessage());
                notificationData.put("idNotification", notification.getId().toString());
                notificationData.put("type", notification.getType().toString());
                messageData.put("notificationData", notificationData);
                messageBody.put("message", messageData);
                // Convert to JSON
                Gson gson = new Gson();
                String jsonBody = gson.toJson(messageBody);
                // Create the request
                HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
                // Send the request to Firebase
                ResponseEntity<String> response = restTemplate.exchange(
                        FIREBASE_API_URL,
                        HttpMethod.POST,
                        request,
                        String.class);
                // Log the response (optional)
                System.out.println("Firebase response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error sending Firebase notification: " + e.getMessage());
            }
        }
    }


    
    @Async
    public void sendFirebaseNotification(RestaurantNotification notification) {
        RestaurantUser user = notification.getRestaurantUser();
        List<CustomerFcmToken> tokens = customerFcmTokenService.getTokensByCustomerId(user.getId());
        for (CustomerFcmToken token : tokens) {
            try {
                googleCredentials.refreshIfExpired();
                String accessToken = googleCredentials.getAccessToken().getTokenValue();
                RestTemplate restTemplate = new RestTemplate();
                // Set HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + accessToken);
                // Create notification body
                Map<String, Object> messageBody = new HashMap<>();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("token", token.getFcmToken());
                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("title", NotificatioUtils.getRestaurantTemplates().get(notification.getType()).getTitle());
                notificationData.put("body", NotificatioUtils.getRestaurantTemplates().get(notification.getType()).getMessage());
                notificationData.put("idNotification", notification.getId().toString());
                notificationData.put("type", notification.getType().toString());
                messageData.put("notificationData", notificationData);
                messageBody.put("message", messageData);
                // Convert to JSON
                Gson gson = new Gson();
                String jsonBody = gson.toJson(messageBody);
                // Create the request
                HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
                // Send the request to Firebase
                ResponseEntity<String> response = restTemplate.exchange(
                        FIREBASE_API_URL,
                        HttpMethod.POST,
                        request,
                        String.class);
                // Log the response (optional)
                System.out.println("Firebase response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error sending Firebase notification: " + e.getMessage());
            }
        }
    }

    public Optional<String> getOldTokenIfPresent(String deviceId) {
        CustomerFcmToken token = customerFcmTokenService.getTokenByDeviceId(deviceId);
        return Optional.of(token.getFcmToken());
    }
    @Async
    public void sendFirebaseRestaurantNotification(String title, String body, Long idRestaurantUser) {
        RestaurantUser restaurantUser = restaurantUserDAO.findById(idRestaurantUser)
            .orElseThrow(() -> new RuntimeException("RestaurantUser not found with id: " + idRestaurantUser));
        RestaurantUser user = restaurantUser;
        Long idUser = user.getId();
        List<CustomerFcmToken> tokens = customerFcmTokenService.getTokensByCustomerId(idUser);
        for (CustomerFcmToken token : tokens) {
            try {
                googleCredentials.refreshIfExpired();
                String accessToken = googleCredentials.getAccessToken().getTokenValue();
                RestTemplate restTemplate = new RestTemplate();
                // Set HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + accessToken);
                // Create notification body
                Map<String, Object> messageBody = new HashMap<>();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("token", token.getFcmToken());
                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("title", title);
                notificationData.put("body", body);
                messageData.put("notificationData", notificationData);
                messageBody.put("message", messageData);
                // Convert to JSON
                Gson gson = new Gson();
                String jsonBody = gson.toJson(messageBody);
                // Create the request
                HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
                // Send the request to Firebase
                ResponseEntity<String> response = restTemplate.exchange(
                        FIREBASE_API_URL,
                        HttpMethod.POST,
                        request,
                        String.class);
                // Log the response (optional)
                System.out.println("Firebase response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error sending Firebase notification: " + e.getMessage());
            }
        }
    }

    @Async
    public void sendFirebaseCustomerNotification(String title, String body, Long idUser) {
        List<CustomerFcmToken> tokens = customerFcmTokenService.getTokensByCustomerId(idUser);
        for (CustomerFcmToken token : tokens) {
            try {
                googleCredentials.refreshIfExpired();
                String accessToken = googleCredentials.getAccessToken().getTokenValue();
                RestTemplate restTemplate = new RestTemplate();
                // Set HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + accessToken);
                // Create notification body
                Map<String, Object> messageBody = new HashMap<>();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("token", token.getFcmToken());
                Map<String, String> notificationData = new HashMap<>();
                notificationData.put("title", title);
                notificationData.put("body", body);
                messageData.put("notificationData", notificationData);
                messageBody.put("message", messageData);
                // Convert to JSON
                Gson gson = new Gson();
                String jsonBody = gson.toJson(messageBody);
                // Create the request
                HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
                // Send the request to Firebase
                ResponseEntity<String> response = restTemplate.exchange(
                        FIREBASE_API_URL,
                        HttpMethod.POST,
                        request,
                        String.class);
                // Log the response (optional)
                System.out.println("Firebase response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error sending Firebase notification: " + e.getMessage());
            }
        }
    }

}