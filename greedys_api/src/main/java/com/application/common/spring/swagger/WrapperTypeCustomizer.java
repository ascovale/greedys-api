package com.application.common.spring.swagger;

import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.annotation.Order;

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
@Order(1000) // Execute AFTER all default SpringDoc customizers and OperationCustomizer
public class WrapperTypeCustomizer implements OpenApiCustomizer {

    private final String apiGroup;
    private final BaseMetadataSchemaProvider baseMetadataProvider;
    private final WrapperTypeRegistry wrapperTypeRegistry;
    private final DataTypeSchemaGenerator dataTypeSchemaGenerator;
    private final WrapperSchemaGeneratorHelper wrapperSchemaGenerator;
    private final OperationResponseUpdater operationResponseUpdater;

    /**
     * Constructor with API group name and specific WrapperTypeRegistry
     */
    public WrapperTypeCustomizer(String apiGroup, WrapperTypeRegistry wrapperTypeRegistry) {
        this.apiGroup = apiGroup;
        this.baseMetadataProvider = new BaseMetadataSchemaProvider();
        this.wrapperTypeRegistry = wrapperTypeRegistry;
        this.dataTypeSchemaGenerator = new DataTypeSchemaGenerator();
        this.wrapperSchemaGenerator = new WrapperSchemaGeneratorHelper();
        this.operationResponseUpdater = new OperationResponseUpdater();
        log.debug("Created WrapperTypeCustomizer for API group: {}", apiGroup);
    }

    /**
     * Metodo principale di customizzazione OpenAPI
     * Coordina le 6 fasi del processo di generazione schemi
     */
    @Override
    public void customise(OpenAPI openApi) {
        
        try {
            // FASE 1: Setup e validazione iniziale
            if (!setupInitialConfiguration(openApi)) {
                return;
            }
            
            // FASE 2: Raccolta di tutti i wrapper types dalle operations
            Set<WrapperTypeInfo> wrapperTypes = collectAllWrapperTypes(openApi);
            if (wrapperTypes.isEmpty()) {
                // log.warn("⚠️ CUSTOMIZER: Nessun wrapper type trovato!");
                return;
            }
            
            // 🎯 TRACE AuthResponseDTO - verifica presenza dopo raccolta
            boolean authFound = false;
            for (WrapperTypeInfo info : wrapperTypes) {
                if (info.dataClassName.contains("AuthResponseDTO")) {
                    log.warn("🎯 FASE2-AuthResponseDTO: CONFIRMED in collected wrapperTypes! schema={}", 
                        info.getWrapperSchemaName());
                    authFound = true;
                }
            }
            if (!authFound) {
                log.warn("🎯 FASE2-AuthResponseDTO: NOT FOUND in collected wrapperTypes!");
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
            log.error("⚠️ CUSTOMIZER: Errore durante customizzazione: {}", e.getMessage(), e);
        }
    }

    /**
     * FASE 1: Setup iniziale e validazione struttura OpenAPI
     * Configura la struttura base e pulisce i registries per una nuova elaborazione
     */
    private boolean setupInitialConfiguration(OpenAPI openApi) {
        // Valida struttura OpenAPI
        if (!baseMetadataProvider.validateOpenApiStructure(openApi)) {
            return false;
        }
        // Setup metadati base
        baseMetadataProvider.setupBaseMetadata(openApi);
        baseMetadataProvider.addCommonSchemas(openApi);
        
        // log.warn("⚠️ SETUP: Inizializzazione completata, schemi esistenti: {}", openApi.getComponents().getSchemas().size());
        return true;
    }

    /**
     * FASE 2: Raccolta wrapper types
     * Scansiona tutte le API operations per trovare wrapper types configurati dall'OperationCustomizer
     */
    private Set<WrapperTypeInfo> collectAllWrapperTypes(OpenAPI openApi) {
        Set<WrapperTypeInfo> wrapperTypes = wrapperTypeRegistry.collectAllWrapperTypes(openApi);
        return wrapperTypes;
    }

    /**
     * FASE 3: Generazione schemi dati
     * Crea schemi SpringDoc per tutti i tipi T unici estratti dai wrapper types
     */
    private void generateDataTypeSchemas(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        dataTypeSchemaGenerator.generateDataTypeSchemas(wrapperTypes, openApi, wrapperTypeRegistry);
    }

    /**
     * FASE 4: Generazione schemi wrapper
     * Crea schemi wrapper che utilizzano $ref per referenziare i tipi T
     * ResponseWrapperErrorDetails e schemi comuni sono già stati generati da BaseMetadataSchemaProvider
     */
    private void generateWrapperSchemas(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        // ✅ Generate only specific wrapper schemas (ResponseWrapperCustomerDTO, etc.)
        // ResponseWrapperErrorDetails and common schemas already generated by BaseMetadataSchemaProvider.addCommonSchemas()
        wrapperSchemaGenerator.generateWrapperSchemas(wrapperTypes, openApi, wrapperTypeRegistry);
    }

    /**
     * FASE 5: Aggiornamento response operations
     * Sostituisce le response inline con riferimenti ai nuovi schemi nominati
     * E aggiunge success responses mancanti dal Registry
     */
    private void updateOperationResponses(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        operationResponseUpdater.updateOperationResponses(wrapperTypes, wrapperTypeRegistry, openApi);
    }

    /**
     * FASE 6: Validazione finale
     * Verifica che tutti gli schemi necessari siano stati creati e registra statistiche
     */
    private void validateFinalResult(OpenAPI openApi) {
        int finalSchemaCount = openApi.getComponents().getSchemas().size();
        // log.warn("⚠️ FINALE: Processo completato, schemi totali finali: {}", finalSchemaCount);
        
        // Lista tutti gli schemi per vedere cosa è stato generato
        openApi.getComponents().getSchemas().keySet().forEach(schemaName -> {
            if (schemaName.startsWith("ResponseWrapper")) {
                // log.warn("⚠️ FINALE: Schema wrapper trovato: {}", schemaName);
            }
        });
    }
}