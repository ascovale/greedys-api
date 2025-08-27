package com.application.common.spring.swagger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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
     * con riferimenti specifici ai wrapper schemas E aggiunge success responses mancanti
     * 
     * @param wrapperTypes Set di wrapper types disponibili per il mapping
     * @param wrapperTypeRegistry Registry contenente le success responses
     * @param openApi OpenAPI da aggiornare
     */
    public void updateOperationResponses(Set<WrapperTypeInfo> wrapperTypes, WrapperTypeRegistry wrapperTypeRegistry, OpenAPI openApi) {
        
        if (openApi.getPaths() == null) {
            // log.warn("⚠️ RESPONSE_UPDATER: Nessun path trovato nell'OpenAPI");
            return;
        }
        
        AtomicInteger updatedResponses = new AtomicInteger(0);
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger addedSuccessResponses = new AtomicInteger(0);
        
        // log.warn("⚠️ RESPONSE_UPDATER: Inizio aggiornamento responses con {} wrapper types disponibili", wrapperTypes.size());
        
        // Scansiona tutti i path e le loro operations
        openApi.getPaths().forEach((pathUrl, pathItem) -> {
            
            // Controlla tutte le HTTP operations del path
            pathItem.readOperations().forEach(operation -> {
                totalOperations.incrementAndGet();
                
                // NEW: Prima aggiungiamo le success responses mancanti usando operationId direttamente
                String operationId = operation.getOperationId();
                if (operationId != null && !operationId.trim().isEmpty()) {
                    var successResponses = wrapperTypeRegistry.getSuccessResponsesByOperationId(operationId);
                    if (!successResponses.isEmpty()) {
                        int added = addMissingSuccessResponses(operation, successResponses);
                        addedSuccessResponses.addAndGet(added);
                        
                        log.debug("✅ RESPONSE_UPDATER: Found {} success responses for operationId '{}'", 
                            successResponses.size(), operationId);
                    } else {
                        log.debug("⚠️ RESPONSE_UPDATER: No success responses registered for operationId '{}'", operationId);
                    }
                    
                } else {
                    // log.warn("⚠️ RESPONSE_UPDATER: Operation has no operationId, skipping success response addition");
                }
                
                // NEW: Aggiungi e correggi error responses standardizzate - SEMPRE, anche senza operationId
                int addedErrors = addStandardErrorResponses(operation);
                log.debug("✅ RESPONSE_UPDATER: Fixed {} error responses for operation '{}' (operationId: {})", 
                    addedErrors, 
                    operation.getSummary() != null ? operation.getSummary() : "unknown",
                    operationId != null ? operationId : "none");
                
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
        
        log.warn("⚠️ RESPONSE_UPDATER: Processo completato - {} operations processate, {} responses aggiornate, {} success responses aggiunte", 
            totalOperations.get(), updatedResponses.get(), addedSuccessResponses.get());
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
    
    /**
     * NEW: Aggiunge success responses mancanti all'operazione
     */
    private int addMissingSuccessResponses(io.swagger.v3.oas.models.Operation operation, java.util.List<ResponseInfo> successResponses) {
        int addedCount = 0;
        
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        
        for (ResponseInfo responseInfo : successResponses) {
            String code = responseInfo.getCode();
            
            // 🎯 DEBUG per response 201
            if ("201".equals(code)) {
                log.warn("🔍 DEBUG-201: Processing 201 response for operationId='{}', wrapperSchemaRef='{}'", 
                    operation.getOperationId(), responseInfo.getWrapperSchemaRef());
            }
            
            // Controlla se la response è già presente
            if (!operation.getResponses().containsKey(code)) {
                // Crea la nuova success response
                ApiResponse apiResponse = new ApiResponse()
                    .description(responseInfo.getDescription());
                
                // Aggiungi content con schema reference
                if (responseInfo.getWrapperSchemaRef() != null) {
                    Schema<?> schema = new Schema<>().$ref(responseInfo.getWrapperSchemaRef());
                    
                    // 🎯 DEBUG per response 201
                    if ("201".equals(code)) {
                        log.warn("🔍 DEBUG-201: Creating schema with $ref='{}' for operationId='{}'", 
                            responseInfo.getWrapperSchemaRef(), operation.getOperationId());
                    }
                    
                    MediaType mediaType = new MediaType().schema(schema);
                    Content content = new Content().addMediaType("application/json", mediaType);
                    apiResponse.content(content);
                } else {
                    // 🎯 DEBUG per response 201 - caso NULL
                    if ("201".equals(code)) {
                        log.error("❌ DEBUG-201: wrapperSchemaRef is NULL for operationId='{}'! No schema will be added.", 
                            operation.getOperationId());
                    }
                }
                
                // Aggiungi la response all'operazione
                operation.getResponses().addApiResponse(code, apiResponse);
                addedCount++;
                
                // 🎯 DEBUG per response 201
                if ("201".equals(code)) {
                    log.warn("✅ DEBUG-201: Added 201 response to operationId='{}' with description='{}'", 
                        operation.getOperationId(), responseInfo.getDescription());
                }
                
                log.debug("✅ RESPONSE_UPDATER: Added {} response to {}: {}", 
                    code, operation.getOperationId() != null ? operation.getOperationId() : "unknown", 
                    responseInfo.getDescription());
            } else {
                // 🎯 DEBUG per response 201 - caso già presente
                if ("201".equals(code)) {
                    log.warn("⚠️ DEBUG-201: Response 201 already exists for operationId='{}', skipping", 
                        operation.getOperationId());
                }
            }
        }
        
        return addedCount;
    }
    
    /**
     * NEW: Aggiunge e corregge error responses standardizzate con schema consolidati oneOf
     * FIXED: Context-aware per gestire CREATE vs READ operations con error responses appropriati
     */
    private int addStandardErrorResponses(io.swagger.v3.oas.models.Operation operation) {
        AtomicInteger fixedCount = new AtomicInteger(0);
        String errorSchemaRef = "#/components/schemas/ResponseWrapperError"; // ✅ FIXED: Use dedicated error schema
        
        // Determina il tipo di operation dai success response codes esistenti
        java.util.Map<String, String> errorResponses = determineErrorResponses(operation);
        
        if (errorResponses.isEmpty()) {
            log.debug("⚠️ RESPONSE_UPDATER: No error responses to apply for operation {}", 
                operation.getOperationId() != null ? operation.getOperationId() : "unknown");
            return 0;
        }
        
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        
        for (java.util.Map.Entry<String, String> error : errorResponses.entrySet()) {
            String code = error.getKey();
            String description = error.getValue();
            
            // Controlla se la error response esiste e ha schema sbagliato
            ApiResponse existingResponse = operation.getResponses().get(code);
            
            if (existingResponse != null && existingResponse.getContent() != null) {
                // Correggi il riferimento schema se sbagliato
                existingResponse.getContent().forEach((mediaType, mediaTypeObject) -> {
                    if (mediaTypeObject.getSchema() != null && 
                        mediaTypeObject.getSchema().get$ref() != null) {
                        
                        String currentRef = mediaTypeObject.getSchema().get$ref();
                        
                        // Se usa schema sbagliato (non consolidato), correggilo
                        if (!currentRef.equals(errorSchemaRef) && 
                            (currentRef.contains("ResponseWrapper") && 
                             !currentRef.contains("ResponseWrapperSingle") && 
                             !currentRef.contains("ResponseWrapperList") && 
                             !currentRef.contains("ResponseWrapperPage"))) {
                            
                            mediaTypeObject.getSchema().set$ref(errorSchemaRef);
                            fixedCount.incrementAndGet();
                            
                            log.debug("🔧 RESPONSE_UPDATER: Fixed error schema {} {} → {} replaced with {}", 
                                operation.getOperationId() != null ? operation.getOperationId() : "unknown",
                                code, currentRef, errorSchemaRef);
                        }
                    }
                });
            } else if (existingResponse == null) {
                // Aggiungi error response mancante
                ApiResponse errorResponse = new ApiResponse()
                    .description(description);
                
                Schema<?> schema = new Schema<>().$ref(errorSchemaRef);
                MediaType mediaType = new MediaType().schema(schema);
                Content content = new Content().addMediaType("application/json", mediaType);
                errorResponse.content(content);
                
                operation.getResponses().addApiResponse(code, errorResponse);
                fixedCount.incrementAndGet();
                
                log.debug("➕ RESPONSE_UPDATER: Added missing error response {} {} with schema {}", 
                    operation.getOperationId() != null ? operation.getOperationId() : "unknown",
                    code, errorSchemaRef);
            }
        }
        
        return fixedCount.get();
    }
    
    /**
     * Determina gli error responses appropriati basandosi sul tipo di operation
     * 
     * LOGICA:
     * - Se ha response 201 → CREATE operation → errori: 400, 401, 403, 409, 500  
     * - Se ha response 200 → READ operation → errori: 400, 401, 403, 404, 500
     * - Altrimenti → Standard operation → errori: 400, 401, 403, 500
     */
    private java.util.Map<String, String> determineErrorResponses(io.swagger.v3.oas.models.Operation operation) {
        
        // Controlla i success response codes esistenti
        boolean hasCreated = false;
        boolean hasOk = false;
        
        if (operation.getResponses() != null) {
            hasCreated = operation.getResponses().containsKey("201");
            hasOk = operation.getResponses().containsKey("200");
        }
        
        if (hasCreated) {
            // CREATE operation (executeCreate) - @CreateApiResponses
            return java.util.Map.of(
                "400", "Bad Request - Invalid input parameters",
                "401", "Unauthorized - Authentication required", 
                "403", "Forbidden - Access denied",
                "409", "Conflict - Resource already exists",
                "500", "Internal Server Error"
            );
        } else if (hasOk) {
            // READ operation (execute, executeVoid, executeList, executePaginated) - @ReadApiResponses  
            return java.util.Map.of(
                "400", "Bad Request - Invalid input parameters",
                "401", "Unauthorized - Authentication required",
                "403", "Forbidden - Access denied", 
                "404", "Not Found - Resource not found",
                "500", "Internal Server Error"
            );
        } else {
            // Standard operation fallback - @StandardApiResponses
            return java.util.Map.of(
                "400", "Bad Request - Invalid input parameters",
                "401", "Unauthorized - Authentication required",
                "403", "Forbidden - Access denied",
                "500", "Internal Server Error"
            );
        }
    }
}
