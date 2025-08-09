package com.application.common.spring;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"docker", "prod"}) // 🔧 Attiva SSL solo per Docker e Produzione (NON dev)
public class SSLConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        System.out.println("🔒 SSLConfig ATTIVATO - Profilo: " + System.getProperty("spring.profiles.active"));
        return factory -> {
            Ssl ssl = new Ssl();
            ssl.setKeyStore("/run/secrets/keystore");
            ssl.setKeyStoreType("PKCS12");
            ssl.setKeyAlias("api.greedys.it");

            // Leggi la password del keystore direttamente dal file Docker secret
            try (BufferedReader br = new BufferedReader(new FileReader("/run/secrets/keystore_password"))) {
                String keyStorePassword = br.readLine();
                ssl.setKeyStorePassword(keyStorePassword);
                System.out.println("✅ SSL configurato correttamente");
            } catch (IOException e) {
                System.err.println("❌ Errore lettura keystore: " + e.getMessage());
                throw new RuntimeException("Errore durante la lettura della password del keystore", e);
            }

            factory.setSsl(ssl);
            factory.setPort(8443);
        };
    }
}
