package com.application.common.spring.swagger.customizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Customizer specifico per modificare le definizioni globali dei tag nell'OpenAPI.
 * Aggiunge prefissi ai tag globali per farli corrispondere con quelli usati negli endpoint.
 * 
 * Deve essere chiamato DOPO il MetadataCollector per sincronizzare i tag globali con quelli delle operazioni.
 */
@Component
@Order(600) // Dopo il MetadataCollector (500)
@Slf4j
public class TagDefinitionCustomizer implements OpenApiCustomizer {
    
    private final String groupPrefix;
    
    public TagDefinitionCustomizer() {
        this.groupPrefix = null; // Verrà configurato nel SwaggerConfig
    }
    
    public TagDefinitionCustomizer(String groupPrefix) {
        this.groupPrefix = groupPrefix;
    }
    
    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            log.debug("No paths found, skipping tag customization");
            return;
        }

        String prefix = determinePrefix();
        if (prefix == null) {
            log.debug("No prefix determined, skipping tag customization");
            return;
        }

        log.error("🔧 TAG CUSTOMIZER CHIAMATO! Group: {} - Processing global tag definitions...", prefix);
        
        // Raccoglie tutti i tag usati nelle operazioni
        Set<String> operationTags = new HashSet<>();
        openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperations().forEach(operation -> {
                if (operation.getTags() != null) {
                    operationTags.addAll(operation.getTags());
                }
            });
        });
        
        // Crea le definizioni globali dei tag basandosi sui tag usati nelle operazioni
        List<Tag> globalTags = new ArrayList<>();
        for (String tagName : operationTags) {
            Tag tag = new Tag()
                .name(tagName)
                .description("Operations for " + tagName);
            globalTags.add(tag);
            log.error("🏷️ Created global tag definition: '{}'", tagName);
        }
        
        // Sostituisci i tag globali con quelli raccolti dalle operazioni
        openApi.setTags(globalTags);
        
        log.error("🎉 Global tag definition customization completed. Created {} tag definitions for group {}", 
            globalTags.size(), prefix);
    }
    
    /**
     * Determina il prefisso da usare basandosi sul gruppo configurato.
     */
    private String determinePrefix() {
        return groupPrefix;
    }
}