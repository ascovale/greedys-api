package com.application.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Pre-carica le specifiche OpenAPI all'avvio dell'applicazione
 * per evitare i lunghi tempi di attesa al primo accesso a Swagger.
 * 
 * Attivo nei profili di sviluppo e Docker.
 * Esegue PRIMA del logo dell'applicazione.
 */
@Component
@Slf4j
@Profile({ "dev", "dev-minimal", "docker" })
public class OpenApiStartupConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private Environment environment;

    @Override
    @Order(1) // Esegue PRIMA del logo
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String port = environment.getProperty("server.port", "8080");
        String baseUrl = "http://localhost:" + port;

        // Pre-carica tutte le specifiche OpenAPI in background
        new Thread(() -> {
            try {
                log.info("🚀 Pre-caricamento specifiche OpenAPI...");

                RestTemplate restTemplate = new RestTemplate();

                // Pre-carica Admin API (PRIMA)
                preloadApiSpec(restTemplate, baseUrl + "/v3/api-docs/admin-api", "Admin API");

                // Pre-carica Restaurant API
                preloadApiSpec(restTemplate, baseUrl + "/v3/api-docs/restaurant-api", "Restaurant API");

                // Pre-carica Customer API
                preloadApiSpec(restTemplate, baseUrl + "/v3/api-docs/customer-api", "Customer API");

                log.info("✅ Tutte le specifiche OpenAPI sono state pre-caricate! Swagger dovrebbe ora essere veloce.");
                log.info("                                   \\   \r\n" + //
                        "   ____                   _           \r\n" + //
                        "  / ___|_ __ ___  ___  __| |_   _ ___ \r\n" + //
                        " | |  _| '__/ _ \\/ _ \\/ _` | | | / __|\r\n" + //
                        " | |_| | | |  __/  __/ (_| | |_| \\__ \\\r\n" + //
                        "  \\____|_|  \\___|\\___|\\__,_|\\__, |___/\r\n" + //
                        "                            |___/     ");
                log.info("\n\n📋 Restaurant Reservation Api v1.0.0");
                
                log.info("\n\n🔐 CORS CONFIGURATION STATUS:");
                log.info("  ✅ /ws/** (WebSocket) → allowCredentials=true, allowOrigins=[http://*, https://*]");
                log.info("  ✅ /** (Default) → allowCredentials=false, allowOrigins=[*]");

                log.info("\n\n✅ ✅ ✅ --- APPLICATION SUCCESSFULLY STARTED --- ✅ ✅ ✅");

            } catch (Exception e) {
                log.warn("⚠️  Errore durante il pre-caricamento delle specifiche OpenAPI: {}", e.getMessage());
            }
        }).start();
    }

    private void preloadApiSpec(RestTemplate restTemplate, String url, String apiName) {
        try {
            long startTime = System.currentTimeMillis();
            restTemplate.getForObject(url, String.class);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("📄 {} caricata in {} ms", apiName, duration);
        } catch (Exception e) {
            log.warn("❌ Errore caricamento {}: {}", apiName, e.getMessage());
        }
    }
}
