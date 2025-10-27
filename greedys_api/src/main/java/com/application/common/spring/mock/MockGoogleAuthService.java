package com.application.common.spring.mock;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.application.common.service.authentication.GoogleAuthService;
import com.application.common.web.dto.security.AuthRequestGoogleDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock Google Auth Service per sviluppo minimal
 * Si attiva solo quando google.oauth.enabled=false
 */
@Service
@Primary
@ConditionalOnProperty(name = "google.oauth.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockGoogleAuthService extends GoogleAuthService {

    public MockGoogleAuthService() {
        super();
        log.warn("🔧 MOCK: GoogleAuthService attivato - modalità sviluppo minimal");
    }

    @Override
    public <U> AuthResponseDTO authenticateWithGoogle(
            AuthRequestGoogleDTO authRequest, 
            Function<String, U> userFun, 
            BiFunction<String, GoogleIdToken, U> createUserFun,
            Function<U, AuthResponseDTO> responseBuilder) throws Exception {
        
        log.info("🔧 MOCK: Google authentication chiamato");
        log.info("   🎫 Token: {}", authRequest.getToken() != null ? 
                authRequest.getToken().substring(0, Math.min(10, authRequest.getToken().length())) + "..." : "null");
        
        if (authRequest.getToken() == null || authRequest.getToken().trim().isEmpty()) {
            log.warn("🔧 MOCK: Token Google vuoto o null - autenticazione fallita");
            throw new Exception("Google token verification failed (mock)");
        }
        
        // Simula un'email basata sul token per sviluppo
        String mockEmail = extractMockEmailFromToken(authRequest.getToken());
        log.info("   📧 Email estratta (mock): {}", mockEmail);
        
        // Cerca utente esistente
        U user = userFun.apply(mockEmail);
        
        if (user == null) {
            log.info("🔧 MOCK: Utente non trovato, creazione nuovo utente");
            // Crea mock GoogleIdToken
            GoogleIdToken mockToken = createMockGoogleIdToken(mockEmail);
            user = createUserFun.apply(mockEmail, mockToken);
            log.info("✅ MOCK: Nuovo utente creato");
        } else {
            log.info("✅ MOCK: Utente esistente trovato");
        }
        
        log.info("✅ MOCK: Autenticazione Google completata con successo");
        return responseBuilder.apply(user);
    }

    /**
     * Estrae email mock dal token per sviluppo
     * Formato token: "mock-email@domain.com" oppure usa default
     */
    private String extractMockEmailFromToken(String token) {
        // Se il token contiene @ assumiamo sia già un'email
        if (token.contains("@")) {
            return token.toLowerCase().trim();
        }
        
        // Altrimenti crea email mock basata su hash del token
        String emailPrefix = "dev-user-" + Math.abs(token.hashCode() % 10000);
        return emailPrefix + "@mock-google.dev";
    }

    /**
     * Crea un GoogleIdToken mock per testing
     * Nota: GoogleIdToken è complesso da mockare, quindi restituiamo null
     * Il codice chiamante deve gestire null come caso valido in modalità mock
     */
    private GoogleIdToken createMockGoogleIdToken(String email) {
        log.debug("🔧 MOCK: GoogleIdToken creato (mock) per email: {}", email);
        // GoogleIdToken è troppo complesso da creare manualmente
        // Il servizio chiamante deve gestire null come caso valido
        return null;
    }
}
