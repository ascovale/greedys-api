package com.application.common.spring;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev") // 🚀 Solo per profilo dev
public class DevServerConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> devServletContainerCustomizer() {
        System.out.println("🚀 DevServerConfig ATTIVATO - Modalità sviluppo HTTP");
        return factory -> {
            // Configurazione HTTP semplice per sviluppo
            factory.setPort(8080);
            // Nessuna configurazione SSL
            System.out.println("✅ Server HTTP configurato per sviluppo su porta 8080");
        };
    }
}
