package com.application.service;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

@Service
@Transactional
public class RestaurantUserFirebaseService {

    private static final String PROJECT_ID ="greedy-36dec";
    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";
    private static final String FIREBASE_API_URL = "https://fcm.googleapis.com/v1/projects/"+PROJECT_ID+"/messages:send";
    private GoogleCredentials googleCredentials;
    private final RestaurantUserFcmTokenService restaurantUserFcmTokenService;
    private RestaurantUserDAO restaurantUserDAO;

    public RestaurantUserFirebaseService(RestaurantUserDAO restaurantUserDAO, RestaurantUserFcmTokenService restaurantUserFcmTokenService)
     {
        this.restaurantUserDAO = restaurantUserDAO;
        try {
            this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream("/run/secrets/service_account"))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            this.googleCredentials.refreshIfExpired();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google Credentials: " + e.getMessage(), e);
        }
        this.restaurantUserFcmTokenService = restaurantUserFcmTokenService;
    }

    public FirebaseToken verifyToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            // Handle token verification error
            return null;
        }
    }
    /* 
    @Async
    public void sendFirebaseNotification(Notification notification) {
        Customer user = notification.getCustomer();
        List<CustomerFcmToken> tokens = restaurantUserFcmTokenService.getTokensByRestaurantUserId(user.getId());
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
        List<CustomerFcmToken> tokens = restaurantUserFcmTokenService.getTokensByCustomerId(user.getId());
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
        CustomerFcmToken token = restaurantUserFcmTokenService.getTokenByDeviceId(deviceId);
        return Optional.of(token.getFcmToken());
    }
    @Async
    public void sendFirebaseRestaurantNotification(String title, String body, Long idRestaurantUser) {
        RestaurantUser restaurantUser = restaurantUserDAO.findById(idRestaurantUser)
            .orElseThrow(() -> new RuntimeException("RestaurantUser not found with id: " + idRestaurantUser));
        RestaurantUser user = restaurantUser;
        Long idUser = user.getId();
        List<CustomerFcmToken> tokens = restaurantUserFcmTokenService.getTokensByCustomerId(idUser);
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
        List<CustomerFcmToken> tokens = restaurantUserFcmTokenService.getTokensByCustomerId(idUser);
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
*/
}