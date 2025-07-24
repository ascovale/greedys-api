package com.application.common.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.service.CustomerFcmTokenService;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.service.RUserFcmTokenService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class FirebaseService {

    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";
    private GoogleCredentials googleCredentials;

    public FirebaseService(CustomerFcmTokenService customerFcmTokenService,RUserDAO RUserDAO, RUserFcmTokenService restaurantFcmTokenService)
     {
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
            log.warn("Token verification failed: {}", e.getMessage());
            return null;
        }
    }

    @Async
    public void sendNotification(String title, String body, Map<String, String> data, Collection<String> tokens) {
        tokens.parallelStream().forEach(token -> {
            try {
                Message msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .build();
                FirebaseMessaging.getInstance().send(msg);
            } catch (Exception e) {
                // Handle exceptions, e.g., log the error
                log.error("Failed to send notification to token: {}", token, e);
            }
        });
    }
    
}