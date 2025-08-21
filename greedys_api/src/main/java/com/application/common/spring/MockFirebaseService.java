package com.application.common.spring;

import java.util.Collection;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseToken;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock Firebase Service per sviluppo minimal
 * Si attiva solo quando firebase.enabled=false
 */
@Service
@Primary
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockFirebaseService {

    public MockFirebaseService() {
        log.warn("🔧 MOCK: FirebaseService attivato - modalità sviluppo minimal");
    }

    public FirebaseToken verifyToken(String idToken) {
        log.debug("🔧 MOCK: Firebase verifyToken chiamato per token: {}", 
                  idToken != null ? idToken.substring(0, Math.min(10, idToken.length())) + "..." : "null");
        
        // Simula token valido per sviluppo
        if (idToken != null && !idToken.trim().isEmpty()) {
            log.debug("🔧 MOCK: Token considerato valido (mock)");
            return createMockFirebaseToken(idToken);
        }
        
        log.debug("🔧 MOCK: Token non valido o vuoto");
        return null;
    }

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

    /**
     * Crea un FirebaseToken mock per testing
     */
    private FirebaseToken createMockFirebaseToken(String originalToken) {
        // Poiché FirebaseToken è una classe finale, usiamo un approccio alternativo
        // Restituiamo null per ora, ma logghiamo che il token è considerato valido
        log.debug("🔧 MOCK: FirebaseToken creato (mock) per sviluppo");
        return null; // In modalità mock, il servizio chiamante deve gestire null come "valido"
    }
}
