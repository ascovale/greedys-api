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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Notification;
import com.application.persistence.model.notification.CustomerFcmToken;
import com.application.persistence.model.notification.RestaurantUserFcmToken;
import com.application.persistence.model.restaurant.user.RestaurantNotification;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.service.utils.NotificatioUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.gson.Gson;

@Service
@Transactional
public class FirebaseService {

    private static final String PROJECT_ID ="greedy-36dec";
    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";
    private static final String FIREBASE_API_URL = "https://fcm.googleapis.com/v1/projects/"+PROJECT_ID+"/messages:send";
    private GoogleCredentials googleCredentials;
    private final CustomerFcmTokenService customerFcmTokenService;
    private final RestaurantUserFcmTokenService restaurantFcmTokenService;
    private RestaurantUserDAO restaurantUserDAO;

    public FirebaseService(CustomerFcmTokenService customerFcmTokenService,RestaurantUserDAO restaurantUserDAO, RestaurantUserFcmTokenService restaurantFcmTokenService)
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
        this.restaurantFcmTokenService = restaurantFcmTokenService;
    }

    public FirebaseToken verifyToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            // Handle token verification error
            return null;
        }
    }

    private void sendFcmNotification(String token, String title, String body, String idNotification, String type) {
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
            messageData.put("token", token);

            // Notifica visibile
            Map<String, String> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            messageData.put("notification", notification);

            // Dati custom opzionali
            Map<String, String> data = new HashMap<>();
            if (idNotification != null) data.put("idNotification", idNotification);
            if (type != null) data.put("type", type);
            if (!data.isEmpty()) messageData.put("data", data);

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

    @Async
    public void sendFirebaseNotification(Notification notification) {
        Customer user = notification.getCustomer();
        List<CustomerFcmToken> tokens = customerFcmTokenService.getTokensByCustomerId(user.getId());
        String title = NotificatioUtils.getUserTemplates().get(notification.getType()).getTitle();
        String body = NotificatioUtils.getUserTemplates().get(notification.getType()).getMessage();
        for (CustomerFcmToken token : tokens) {
            sendFcmNotification(token.getFcmToken(), title, body, notification.getId().toString(), notification.getType().toString());
        }
    }

    @Async
    public void sendFirebaseNotification(RestaurantNotification notification) {
        RestaurantUser user = notification.getRestaurantUser();
        List<RestaurantUserFcmToken> tokens = restaurantFcmTokenService.getTokensByRestaurantUserId(user.getId());
        String title = NotificatioUtils.getRestaurantTemplates().get(notification.getType()).getTitle();
        String body = NotificatioUtils.getRestaurantTemplates().get(notification.getType()).getMessage();
        for (RestaurantUserFcmToken token : tokens) {
            sendFcmNotification(token.getFcmToken(), title, body, notification.getId().toString(), notification.getType().toString());
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
                // Notifica visibile
                Map<String, String> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", body);
                messageData.put("notification", notification);
                // Dati custom opzionali
                Map<String, String> data = new HashMap<>();
                messageData.put("data", data);
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
                // Notifica visibile
                Map<String, String> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", body);
                messageData.put("notification", notification);
                // Dati custom opzionali
                Map<String, String> data = new HashMap<>();
                messageData.put("data", data);
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

    /**
     * Invia una notifica di test a un token FCM specifico. Restituisce true se la richiesta va a buon fine, false altrimenti.
     */
    public boolean sendTestNotificationToToken(String fcmToken, String title, String body) {
        try {
            // Qui non servono idNotification e type per il test
            sendFcmNotification(fcmToken, title, body, null, null);
            return true;
        } catch (Exception e) {
            System.err.println("Errore invio notifica di test: " + e.getMessage());
            return false;
        }
    }

}