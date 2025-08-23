package com.application.common.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SecretManager secretManager;

    public FirebaseService(Environment environment, SecretManager secretManager) {
        
        this.secretManager = secretManager;
        
        // Determina la modalità di esecuzione
        log.info("🔧 FirebaseService - Modalità rilevata: {}", secretManager.getExecutionMode());
        
        switch (secretManager.getExecutionMode()) {
            case STANDALONE_DEV:
                initializeStandaloneDev();
                break;
            case DOCKER:
                initializeDockerSecrets();
                break;
            default:
                log.error("❌ FirebaseService - Modalità non riconosciuta: {}", secretManager.getExecutionMode());
                this.googleCredentials = null;
        }
    }

    /**
     * Inizializzazione per modalità standalone (senza Docker)
     */
    private void initializeStandaloneDev() {
        String serviceAccountPath = secretManager.getSecretFilePath("service_account", "firebase.service.account.path");
        
        if (serviceAccountPath != null && Files.exists(Paths.get(serviceAccountPath))) {
            try {
                this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath))
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
                this.googleCredentials.refreshIfExpired();
                log.info("✅ FirebaseService STANDALONE - Credenziali caricate da: {}", serviceAccountPath);
            } catch (IOException e) {
                log.error("❌ FirebaseService STANDALONE - Errore caricamento: {} - Firebase disabilitato", e.getMessage());
                this.googleCredentials = null;
            }
        } else {
            log.warn("⚠️ FirebaseService STANDALONE - Service account non configurato, Firebase disabilitato");
            this.googleCredentials = null;
        }
    }

    /**
     * Inizializzazione per modalità Docker 
     * (stessi secrets per dev e prod - solo il contenuto cambia)
     */
    private void initializeDockerSecrets() {
        try {
            String secretPath = secretManager.getSecretFilePath("service_account", null);
            this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream(secretPath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            this.googleCredentials.refreshIfExpired();
            log.info("✅ FirebaseService DOCKER - Credenziali caricate da: {}", secretPath);
        } catch (IOException e) {
            log.error("❌ FirebaseService DOCKER - Errore caricamento secrets: {}", e.getMessage());
            throw new RuntimeException("Failed to load Google Credentials from Docker secrets: " + e.getMessage(), e);
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
