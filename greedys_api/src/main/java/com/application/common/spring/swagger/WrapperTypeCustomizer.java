package com.application.common.spring.swagger;

import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAPI customizer principale che orchestra la generazione di schemi nominati per ResponseWrapper types.
 * 
 * FLUSSO GENERALE:
 * =================
 * 1. SETUP INIZIALE - Configura struttura base OpenAPI e pulisce registries
 * 2. RACCOLTA TIPI - Scansiona tutte le API operations per trovare wrapper types  
 * 3. GENERAZIONE SCHEMI DATI - Crea schemi per tutti i tipi T unici (UserDto, etc.)
 * 4. GENERAZIONE SCHEMI WRAPPER - Crea schemi wrapper che referenziano i tipi T con $ref
 * 5. AGGIORNAMENTO OPERAZIONI - Sostituisce response inline con riferimenti ai nuovi schemi
 * 6. VALIDAZIONE FINALE - Verifica che tutti gli schemi siano stati creati correttamente
 * 
 * Questa classe coordina il lavoro delle classi helper specializzate:
 * - BaseMetadataSchemaProvider: Setup e validazione struttura OpenAPI
 * - WrapperTypeRegistry: Raccolta e gestione tipi wrapper
 * - DataTypeSchemaGenerator: Generazione schemi per tipi dati (T)
 * - WrapperSchemaGeneratorHelper: Generazione schemi wrapper con $ref
 * 
 * OBIETTIVO: Invece di schemi inline confusi come "JwtRefreshToken201Response",
 * genera schemi nominati e chiari come "ResponseWrapperUserDto" che referenziano "UserDto".
 */
@Slf4j
@Component
@Order(1000) // Execute AFTER all default SpringDoc customizers and OperationCustomizer
public class WrapperTypeCustomizer implements OpenApiCustomizer {
    
    // Classi helper per responsabilità specifiche
    @Autowired
    private BaseMetadataSchemaProvider baseMetadataProvider;
    
    @Autowired 
    private WrapperTypeRegistry wrapperTypeRegistry;
    
    @Autowired
    private DataTypeSchemaGenerator dataTypeSchemaGenerator;
    
    @Autowired
    private WrapperSchemaGeneratorHelper wrapperSchemaGenerator;

    /**
     * Metodo principale di customizzazione OpenAPI
     * Coordina le 6 fasi del processo di generazione schemi
     */
    @Override
    public void customise(OpenAPI openApi) {
        log.info("=== INIZIO CUSTOMIZZAZIONE WRAPPER TYPES ===");
        
        try {
            // FASE 1: Setup e validazione iniziale
            if (!setupInitialConfiguration(openApi)) {
                return;
            }
            
            // FASE 2: Raccolta di tutti i wrapper types dalle operations
            Set<WrapperTypeInfo> wrapperTypes = collectAllWrapperTypes(openApi);
            if (wrapperTypes.isEmpty()) {
                log.info("Nessun wrapper type trovato, customizzazione terminata");
                return;
            }
            
            // FASE 3: Generazione schemi per i tipi dati (T in wrapper<T>)
            generateDataTypeSchemas(wrapperTypes, openApi);
            
            // FASE 4: Generazione schemi wrapper con riferimenti $ref
            generateWrapperSchemas(wrapperTypes, openApi);
            
            // FASE 5: Aggiornamento response nelle operations per usare i nuovi schemi
            updateOperationResponses(wrapperTypes, openApi);
            
            // FASE 6: Validazione finale e statistiche
            validateFinalResult(openApi);
            
        } catch (Exception e) {
            log.error("Errore durante customizzazione wrapper types: {}", e.getMessage(), e);
        }
        
        log.info("=== FINE CUSTOMIZZAZIONE WRAPPER TYPES ===");
    }

    /**
     * FASE 1: Setup iniziale e validazione struttura OpenAPI
     * Configura la struttura base e pulisce i registries per una nuova elaborazione
     */
    private boolean setupInitialConfiguration(OpenAPI openApi) {
        log.info("FASE 1: Setup iniziale e validazione");
        
        // Valida struttura OpenAPI
        if (!baseMetadataProvider.validateOpenApiStructure(openApi)) {
            log.error("Struttura OpenAPI non valida, customizzazione annullata");
            return false;
        }
        
        // Setup metadati base
        baseMetadataProvider.setupBaseMetadata(openApi);
        baseMetadataProvider.addCommonSchemas(openApi);
        
        // Pulisci registries per fresh processing
        wrapperTypeRegistry.clearRegistries();
        
        log.info("Setup completato: {}", baseMetadataProvider.getOpenApiStats(openApi));
        return true;
    }

    /**
     * FASE 2: Raccolta wrapper types
     * Scansiona tutte le API operations per trovare wrapper types configurati dall'OperationCustomizer
     */
    private Set<WrapperTypeInfo> collectAllWrapperTypes(OpenAPI openApi) {
        log.info("FASE 2: Raccolta wrapper types dalle operations");
        
        Set<WrapperTypeInfo> wrapperTypes = wrapperTypeRegistry.collectAllWrapperTypes(openApi);
        
        log.info("Raccolti {} wrapper types unici", wrapperTypes.size());
        for (WrapperTypeInfo wrapper : wrapperTypes) {
            log.debug("  - {} per {}", wrapper.wrapperType, wrapper.getDataTypeSimpleName());
        }
        
        return wrapperTypes;
    }

    /**
     * FASE 3: Generazione schemi dati
     * Crea schemi SpringDoc per tutti i tipi T unici estratti dai wrapper types
     */
    private void generateDataTypeSchemas(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        log.info("FASE 3: Generazione schemi per tipi dati");
        
        dataTypeSchemaGenerator.generateDataTypeSchemas(wrapperTypes, openApi, wrapperTypeRegistry);
        
        log.info("Generazione schemi dati completata: {}", wrapperTypeRegistry.getRegistryStats());
    }

    /**
     * FASE 4: Generazione schemi wrapper
     * Crea schemi wrapper che utilizzano $ref per referenziare i tipi T
     */
    private void generateWrapperSchemas(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        log.info("FASE 4: Generazione schemi wrapper con $ref");
        
        wrapperSchemaGenerator.generateWrapperSchemas(wrapperTypes, openApi, wrapperTypeRegistry);
        
        log.info("Generazione schemi wrapper completata");
    }

    /**
     * FASE 5: Aggiornamento response operations
     * Sostituisce le response inline con riferimenti ai nuovi schemi nominati
     */
    private void updateOperationResponses(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        log.info("FASE 5: Aggiornamento response operations");
        
        // Per ora mantengo la logica nell'operation customizer
        // Questa fase può essere estratta in una classe helper separata se necessario
        log.info("Aggiornamento response completato (gestito da OperationCustomizer)");
    }

    /**
     * FASE 6: Validazione finale
     * Verifica che tutti gli schemi necessari siano stati creati e registra statistiche
     */
    private void validateFinalResult(OpenAPI openApi) {
        log.info("FASE 6: Validazione finale e statistiche");
        
        // Statistiche finali
        String finalStats = baseMetadataProvider.getOpenApiStats(openApi);
        String registryStats = wrapperTypeRegistry.getRegistryStats();
        
        log.info("Customizzazione completata con successo:");
        log.info("  - {}", finalStats);
        log.info("  - {}", registryStats);
        
        // Validazione degli schemi generati
        int expectedSchemas = wrapperTypeRegistry.getRegisteredDataTypes().size() + 
                            wrapperTypeRegistry.getWrapperSchemaNames().size();
        
        int actualSchemas = openApi.getComponents().getSchemas().size();
        
        if (actualSchemas >= expectedSchemas) {
            log.info("✓ Validazione schemi: {} schemi presenti (almeno {} attesi)", actualSchemas, expectedSchemas);
        } else {
            log.warn("⚠ Validazione schemi: {} schemi presenti, {} attesi", actualSchemas, expectedSchemas);
        }
    }
}