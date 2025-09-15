package com.application.common.spring.swagger.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service che espone il JSON del catalogo dei ResponseWrapper generato a startup.
 *
 * Questo service non rigenera il file: si limita a leggerlo dal percorso configurato
 * e a restituirne l'oggetto deserializzato per l'uso nei controller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseWrapperCatalogService {

    private final ObjectMapper objectMapper;

    @Value("${app.swagger.wrapper-catalog.path:target/generated-resources/response-wrappers.json}")
    private String outputPath;

    /**
     * Ritorna il contenuto JSON del catalogo come oggetto deserializzato, oppure null se non disponibile o in caso di errore.
     */
    public Object getCatalog() {
        Path out = Path.of(outputPath);
        try {
            if (!Files.exists(out)) {
                log.warn("Response wrapper catalog not found at {}", out.toAbsolutePath());
                return null;
            }
            byte[] bytes = Files.readAllBytes(out);
            return objectMapper.readValue(bytes, Object.class);
        } catch (IOException e) {
            log.error("Failed to read/parse response wrapper catalog: {}", e.getMessage(), e);
            return null;
        }
    }

}
