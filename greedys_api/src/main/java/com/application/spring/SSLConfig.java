package com.application.spring;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Configuration
public class SSLConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        return factory -> {
            Ssl ssl = new Ssl();
            ssl.setKeyStore("/run/secrets/keystore");
            ssl.setKeyStoreType("PKCS12");
            ssl.setKeyAlias("greedysapi");

            // Leggi la password del keystore direttamente dal file Docker secret
            try (BufferedReader br = new BufferedReader(new FileReader("/run/secrets/keystore_password"))) {
                String keyStorePassword = br.readLine();
                ssl.setKeyStorePassword(keyStorePassword);
            } catch (IOException e) {
                throw new RuntimeException("Errore durante la lettura della password del keystore", e);
            }

            factory.setSsl(ssl);
            factory.setPort(8443);
        };
    }
}
