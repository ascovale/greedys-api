package com.application.common.spring.mock;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.application.common.service.FirebaseService;
import com.application.common.service.SecretManager;
import com.google.firebase.auth.FirebaseToken;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock Firebase Service per sviluppo minimal
 * Estende FirebaseService per compatibilità di tipo
 * Si attiva solo quando firebase.enabled=false
 */
@Service
@Primary
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockFirebaseService extends FirebaseService {

    @Autowired
    public MockFirebaseService(Environment environment, SecretManager secretManager) {
        super(environment, secretManager); // Usa SecretManager autowired
        log.warn("🔧 MOCK: FirebaseService attivato - modalità sviluppo minimal");
        log.info("🔧 MOCK: Firebase completamente disabilitato per profilo dev-minimal");
    }

    /**
     * Mock override of FirebaseService.verifyToken()
     */
    @Override
    public FirebaseToken verifyToken(String idToken) {
        log.debug("🔧 MOCK: Firebase verifyToken chiamato per token: {}", 
                  idToken != null ? idToken.substring(0, Math.min(10, idToken.length())) + "..." : "null");
        
        // Simula token valido per sviluppo
        if (idToken != null && !idToken.trim().isEmpty()) {
            log.debug("🔧 MOCK: Token considerato valido (mock)");
            return null; // Mock token - null indica "valido" in modalità mock
        }
        
        log.debug("🔧 MOCK: Token non valido o vuoto");
        return null;
    }

    /**
     * Mock override of FirebaseService.sendNotification()
     */
    @Override
    @Async
    public void sendNotification(String title, String body, Map<String, String> data, Collection<String> tokens) {
        log.info("🔧 MOCK: Notifica Firebase simulata");
        log.info("   📧 Titolo: {}", title);
        log.info("   📝 Messaggio: {}", body);
        log.info("   📊 Dati: {}", data);
        log.info("   📱 Token destinatari: {} tokens", tokens != null ? tokens.size() : 0);
        
        if (tokens != null && !tokens.isEmpty()) {
            tokens.forEach(token -> 
                log.debug("   🎯 Token: {}...{}", 
                         token.substring(0, Math.min(8, token.length())),
                         token.length() > 8 ? token.substring(token.length() - 4) : "")
            );
        }
        
        log.info("✅ MOCK: Notifica Firebase inviata con successo (simulato)");
    }
}
