package com.application.common.spring.swagger.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Service che espone il JSON del catalogo dei ResponseWrapper generato a startup.
 *
 * Questo service non rigenera il file: si limita a leggerlo dal percorso configurato
 * e a restituirne il contenuto come stringa per l'uso nei controller.
 */
@Component
@Slf4j
public class ResponseWrapperCatalogService {

    @Value("${app.swagger.wrapper-catalog.path:target/generated-resources/response-wrappers.json}")
    private String outputPath;

    /**
     * Ritorna il contenuto JSON del catalogo come stringa, oppure null se non disponibile o in caso di errore.
     */
    public String getCatalog() {
        Path out = Path.of(outputPath);
        try {
            if (!Files.exists(out)) {
                log.warn("Response wrapper catalog not found at {}", out.toAbsolutePath());
                return null;
            }
            return Files.readString(out);
        } catch (IOException e) {
            log.error("Failed to read response wrapper catalog: {}", e.getMessage(), e);
            return null;
        }
    }

}
