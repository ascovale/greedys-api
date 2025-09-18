package com.application.common.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configurazione delle risorse statiche per l'applicazione.
 * 
 * SCOPO PRINCIPALE:
 * Personalizza il mapping delle risorse statiche, in particolare per sovrascrivere
 * la Swagger UI di default con una versione brandizzata/customizzata.
 * 
 * COME FUNZIONA:
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ URL Richiesta: /swagger-ui/index.html                                   │
 * │                                                                         │
 * │ 1️⃣ PRIMA cerca in: classpath:/static/swagger-ui/index.html (CUSTOM)    │
 * │    ✅ Se trova → Serve la versione personalizzata con logo/stili       │
 * │                                                                         │
 * │ 2️⃣ POI cerca in: classpath:/META-INF/resources/webjars/ (DEFAULT)      │
 * │    ✅ Se non trova custom → Fallback alla Swagger UI standard          │
 * └─────────────────────────────────────────────────────────────────────────┘
 * 
 * STRUTTURA DELLE CARTELLE:
 * src/main/resources/
 * ├── static/
 * │   ├── swagger-ui/           👈 TUA VERSIONE CUSTOM (priorità)
 * │   │   ├── index.html        (con logo Greedy's e stili custom)
 * │   │   ├── swagger-ui.css    (stili personalizzati)
 * │   │   └── ...
 * │   ├── logo_api.png          👈 LOGO AZIENDALE
 * │   └── favicon.ico           👈 FAVICON CUSTOM
 * └── META-INF/resources/webjars/
 *     └── swagger-ui/           👈 VERSIONE DEFAULT (dalla dipendenza)
 * 
 * BENEFICI:
 * • 🎨 Swagger UI brandizzata senza modificare i JAR originali
 * • ⚡ Cache ottimizzata per favicon (24h)
 * • 🔄 Fallback automatico alla versione standard se manca la custom
 * • 📱 Logo accessibile direttamente da /logo_api.png
 * 
 * NOTA: Questa configurazione è complementare ai SecurityPatterns e non li sostituisce.
 * I controlli di sicurezza per /swagger-ui/** sono gestiti in SecurityConfig.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapping per logo custom
        registry.addResourceHandler("/logo_api.png")
                .addResourceLocations("classpath:/static/logo_api.png");
        // Mapping per favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400);
        // Regola per servire la custom Swagger UI con priorità sulla cartella custom
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/static/swagger-ui/", "classpath:/META-INF/resources/webjars/");
    }
}
