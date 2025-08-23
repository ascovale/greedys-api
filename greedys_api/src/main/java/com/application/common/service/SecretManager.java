package com.application.common.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Gestione centralizzata dei secrets per l'applicazione.
 * Determina automaticamente se leggere da Docker secrets o da configurazione locale.
 */
@Service
@Slf4j
public class SecretManager {

    private final Environment environment;
    private final ExecutionMode executionMode;

    // Modalità di esecuzione
    public enum ExecutionMode {
        STANDALONE_DEV,  // Esecuzione diretta con IDE/Maven (senza Docker)
        DOCKER           // Docker (stessi secrets per dev e prod)
    }

    public SecretManager(Environment environment) {
        this.environment = environment;
        this.executionMode = determineExecutionMode();
        log.info("🔧 SecretManager inizializzato - Modalità: {}", executionMode);
    }

    /**
     * Determina la modalità di esecuzione basandosi sull'ambiente
     */
    private ExecutionMode determineExecutionMode() {
        // Controlla se i Docker secrets sono disponibili
        boolean dockerSecretsAvailable = Files.exists(Paths.get("/run/secrets"));
        
        if (dockerSecretsAvailable) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isDevProfile = java.util.Arrays.asList(activeProfiles).contains("dev");
            log.info("🐋 Modalità DOCKER - Docker secrets disponibili (profilo: {})", 
                     isDevProfile ? "dev" : "prod");
            return ExecutionMode.DOCKER;
        } else {
            log.info("🖥️  Modalità STANDALONE_DEV - Docker secrets non disponibili");
            return ExecutionMode.STANDALONE_DEV;
        }
    }

    /**
     * Legge un secret generale (JWT, password DB, etc.)
     * @param secretName nome del secret (es: "jwt_secret", "db_password")
     * @param propertyName nome della property di fallback (es: "jwt.secret") 
     * @return valore del secret
     */
    public String readSecret(String secretName, String propertyName) throws IOException {
        switch (executionMode) {
            case DOCKER:
                return readDockerSecret(secretName);
            case STANDALONE_DEV:
                return readPropertySecret(propertyName);
            default:
                throw new IllegalStateException("Modalità di esecuzione non supportata: " + executionMode);
        }
    }

    /**
     * Legge un file secret (service account, certificati, etc.)
     * @param secretName nome del secret file (es: "service_account", "keystore")
     * @param propertyName nome della property con il percorso di fallback
     * @return percorso del file secret
     */
    public String getSecretFilePath(String secretName, String propertyName) {
        switch (executionMode) {
            case DOCKER:
                return "/run/secrets/" + secretName;
            case STANDALONE_DEV:
                String configuredPath = environment.getProperty(propertyName);
                if (configuredPath != null) {
                    return configuredPath;
                } else {
                    log.warn("⚠️ Property '{}' non configurata per modalità standalone", propertyName);
                    return "./dev-secrets/" + secretName; // Fallback
                }
            default:
                throw new IllegalStateException("Modalità di esecuzione non supportata: " + executionMode);
        }
    }

    /**
     * Legge un secret da Docker (/run/secrets/)
     */
    private String readDockerSecret(String secretName) throws IOException {
        String secretPath = "/run/secrets/" + secretName;
        if (!Files.exists(Paths.get(secretPath))) {
            throw new IOException("Docker secret non trovato: " + secretPath);
        }
        String value = Files.readString(Paths.get(secretPath)).trim();
        log.debug("🔐 Secret '{}' letto da Docker (lunghezza: {} caratteri)", secretName, value.length());
        return value;
    }

    /**
     * Legge un secret da Spring properties
     */
    private String readPropertySecret(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (value == null) {
            throw new IllegalArgumentException("Property non trovata: " + propertyName);
        }
        log.debug("🔧 Secret '{}' letto da properties (lunghezza: {} caratteri)", propertyName, value.length());
        return value;
    }

    /**
     * Getter per la modalità di esecuzione (utile per logging/debug)
     */
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * Verifica se siamo in modalità Docker
     */
    public boolean isDockerMode() {
        return executionMode == ExecutionMode.DOCKER;
    }

    /**
     * Verifica se siamo in modalità standalone
     */
    public boolean isStandaloneMode() {
        return executionMode == ExecutionMode.STANDALONE_DEV;
    }
}
