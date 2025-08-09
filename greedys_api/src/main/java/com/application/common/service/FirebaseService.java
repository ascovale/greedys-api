package com.application.common.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.springframework.core.env.Environment;
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

    public FirebaseService(CustomerFcmTokenService customerFcmTokenService, RUserDAO RUserDAO, 
                          RUserFcmTokenService restaurantFcmTokenService, Environment environment) {
        
        // Controlla il profilo attivo
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevProfile = java.util.Arrays.asList(activeProfiles).contains("dev");
        
        if (isDevProfile) {
            // 🚀 PROFILO DEV: Leggi dalle properties (opzionale)
            String firebaseCredentialsPath = environment.getProperty("firebase.service.account.path");
            
            if (firebaseCredentialsPath == null || firebaseCredentialsPath.trim().isEmpty()) {
                log.info("🚀 FirebaseService DEV - Nessun path configurato: Firebase disabilitato");
                this.googleCredentials = null;
            } else {
                try {
                    java.io.File credentialsFile = new java.io.File(firebaseCredentialsPath);
                    if (!credentialsFile.exists()) {
                        log.warn("🚀 FirebaseService DEV - File non trovato: {} - Firebase disabilitato", firebaseCredentialsPath);
                        this.googleCredentials = null;
                    } else {
                        this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream(firebaseCredentialsPath))
                                .createScoped("https://www.googleapis.com/auth/cloud-platform");
                        this.googleCredentials.refreshIfExpired();
                        log.info("✅ FirebaseService DEV - Credenziali caricate da: {}", firebaseCredentialsPath);
                    }
                } catch (IOException e) {
                    log.error("❌ FirebaseService DEV - Errore caricamento: {} - Firebase disabilitato", e.getMessage());
                    this.googleCredentials = null;
                }
            }
        } else {
            // 📦🌐 PROFILO DOCKER/PROD: Usa Docker secrets (path fisso)
            try {
                this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream("/run/secrets/service_account"))
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
                this.googleCredentials.refreshIfExpired();
                log.info("✅ FirebaseService DOCKER/PROD - Credenziali caricate da Docker secrets");
            } catch (IOException e) {
                log.error("❌ FirebaseService DOCKER/PROD - Errore caricamento secrets: {}", e.getMessage());
                throw new RuntimeException("Failed to load Google Credentials from Docker secrets: " + e.getMessage(), e);
            }
        }
    }

    public FirebaseToken verifyToken(String idToken) {
        if (googleCredentials == null) {
            log.debug("🚀 Firebase disabilitato: verifyToken simulato per token: {}", idToken);
            return null; // Ritorna null quando Firebase è disabilitato
        }
        
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
