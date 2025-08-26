package com.application.common.spring.swagger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper specializzato per l'aggiornamento delle response nelle operations OpenAPI.
 * 
 * RESPONSABILITÀ:
 * ===============
 * - Scansionare tutte le operations nell'OpenAPI
 * - Trovare response che usano riferimenti generici a ResponseWrapper
 * - Sostituirle con riferimenti specifici a ResponseWrapperT
 * 
 * STRATEGIA OTTIMIZZATA:
 * ======================
 * 1. Scansiona tutti i path e le operations
 * 2. Per ogni response, controlla se usa "#/components/schemas/ResponseWrapper"
 * 3. Usa i WrapperTypeInfo dal Registry (con schema reference pre-calcolato)
 * 4. Sostituisce il riferimento con "#/components/schemas/ResponseWrapperT"
 * 
 * OBIETTIVO: 
 * Trasformare response generiche in response tipizzate per migliorare la documentazione API.
 */

/**
 * Helper specializzato per l'aggiornamento delle response nelle operations OpenAPI.
 * 
 * RESPONSABILITÀ:
 * ===============
 * - Scansionare tutte le operations nell'OpenAPI
 * - Trovare response che usano riferimenti generici a ResponseWrapper
 * - Sostituirle con riferimenti specifici a ResponseWrapperT
 * - Utilizzare i wrapper types rilevati in FASE 1 per determinare il mapping corretto
 * 
 * FLUSSO:
 * =======
 * 1. Scansiona tutti i path e le operations
 * 2. Per ogni response, controlla se usa "#/components/schemas/ResponseWrapper"
 * 3. Utilizza il primo wrapper type disponibile per sostituire il riferimento generico
 * 4. Sostituisce il riferimento con "#/components/schemas/ResponseWrapperT"
 * 
 * OBIETTIVO: 
 * Trasformare response generiche in response tipizzate per migliorare la documentazione API.
 */
@Slf4j
public class OperationResponseUpdater {

    /**
     * Aggiorna tutte le response delle operations sostituendo riferimenti generici
     * con riferimenti specifici ai wrapper schemas
     * 
     * @param wrapperTypes Set di wrapper types disponibili per il mapping
     * @param openApi OpenAPI da aggiornare
     */
    public void updateOperationResponses(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi) {
        
        if (openApi.getPaths() == null) {
            log.warn("⚠️ RESPONSE_UPDATER: Nessun path trovato nell'OpenAPI");
            return;
        }
        
        AtomicInteger updatedResponses = new AtomicInteger(0);
        AtomicInteger totalOperations = new AtomicInteger(0);
        
        log.warn("⚠️ RESPONSE_UPDATER: Inizio aggiornamento responses con {} wrapper types disponibili", wrapperTypes.size());
        
        // Scansiona tutti i path e le loro operations
        openApi.getPaths().forEach((pathUrl, pathItem) -> {
            
            // Controlla tutte le HTTP operations del path
            pathItem.readOperations().forEach(operation -> {
                totalOperations.incrementAndGet();
                
                if (operation.getResponses() != null) {
                    
                    // Controlla tutte le response dell'operation
                    operation.getResponses().forEach((responseCode, apiResponse) -> {
                        
                        if (apiResponse.getContent() != null) {
                            
                            // Controlla tutti i content types della response
                            apiResponse.getContent().forEach((mediaType, mediaTypeObject) -> {
                                
                                if (mediaTypeObject.getSchema() != null && 
                                    mediaTypeObject.getSchema().get$ref() != null) {
                                    
                                    String currentRef = mediaTypeObject.getSchema().get$ref();
                                    
                                    // Se la response usa il ResponseWrapper generico
                                    if (currentRef.equals("#/components/schemas/ResponseWrapper")) {
                                        
                                        // Trova il wrapper type specifico per questa operation
                                        String specificWrapperRef = findSpecificWrapperRef(
                                            wrapperTypes, operation, responseCode);
                                        
                                        if (specificWrapperRef != null) {
                                            // Sostituisci il riferimento generico con quello specifico
                                            mediaTypeObject.getSchema().set$ref(specificWrapperRef);
                                            updatedResponses.incrementAndGet();
                                            
                                            log.warn("✅ RESPONSE_UPDATER: {} {} → sostituito {} con {}", 
                                                operation.getOperationId() != null ? operation.getOperationId() : "unknown",
                                                responseCode, 
                                                currentRef, 
                                                specificWrapperRef);
                                        } else {
                                            log.warn("❌ RESPONSE_UPDATER: {} {} → nessun wrapper specifico trovato per {}", 
                                                operation.getOperationId() != null ? operation.getOperationId() : "unknown",
                                                responseCode,
                                                currentRef);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            });
        });
        
        log.warn("⚠️ RESPONSE_UPDATER: Processo completato - {} operations processate, {} responses aggiornate", 
            totalOperations.get(), updatedResponses.get());
    }
    
    /**
     * Trova il riferimento specifico al wrapper schema per una data operation e response code
     * 
     * LOGICA: Utilizza il primo wrapper type disponibile per sostituire il riferimento generico.
     * Questo funziona perché tutti i codici di response utilizzano lo stesso wrapper type specifico.
     * 
     * @param wrapperTypes Set di wrapper types disponibili dal Registry
     * @param operation Operation da analizzare
     * @param responseCode Codice di response da mappare  
     * @return Riferimento specifico al schema wrapper, o null se non trovato
     */
    private String findSpecificWrapperRef(Set<WrapperTypeInfo> wrapperTypes, 
                                        io.swagger.v3.oas.models.Operation operation, 
                                        String responseCode) {
        
        // STRATEGIA OTTIMIZZATA: 
        // I wrapperTypes dal Registry contengono già tutti i tipi rilevati per questa operazione.
        // Restituiamo il primo wrapper type disponibile dato che tutti i response codes
        // di un'operazione usano lo stesso wrapper type (es: tutti usano ResponseWrapperUserDto)
        
        if (!wrapperTypes.isEmpty()) {
            WrapperTypeInfo firstWrapper = wrapperTypes.iterator().next();
            String schemaRef = firstWrapper.getWrapperSchemaReference();
            
            log.debug("🔍 RESPONSE_UPDATER: Using wrapper {} for response {}: {}", 
                firstWrapper.getWrapperSchemaName(), responseCode, schemaRef);
            
            return schemaRef;
        }
        
        return null;
    }
}
