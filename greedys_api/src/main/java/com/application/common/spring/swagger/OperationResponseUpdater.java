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
     * Converte una stringa da CamelCase a snake_case
     * 
     * @param camelCase stringa in CamelCase (es. "UserDTO")
     * @return stringa in snake_case (es. "user_dto")
     */
    private String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        return camelCase
            // Prima inserisce underscore prima delle lettere maiuscole
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            // Poi converte tutto in minuscolo
            .toLowerCase();
    }

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
                                            
                                            // ✨ NEW: Aggiungi vendor extensions alla response
                                            WrapperTypeInfo wrapperInfo = wrapperTypes.iterator().next();
                                            addVendorExtensionsToResponse(apiResponse, wrapperInfo);
                                            
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
                
                // ✨ NEW: Aggiungi vendor extensions alla success response
                addVendorExtensionsToSuccessResponse(apiResponse, responseInfo);
                
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
        String errorSchemaRef = "#/components/schemas/ResponseWrapperErrorDetails"; // ✅ FIXED: Use dedicated error schema
        
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
                            
                            // ✨ NEW: Aggiungi vendor extensions statiche per errori
                            addVendorExtensionsToErrorResponse(existingResponse);
                            
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
                
                // ✨ NEW: Aggiungi vendor extensions statiche per errori
                addVendorExtensionsToErrorResponse(errorResponse);
                
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

    /**
     * ✨ NEW: Aggiunge vendor extensions alla response con conversione snake_case
     * 
     * @param response ApiResponse da arricchire con vendor extensions
     * @param wrapperInfo Informazioni del wrapper type per estrarre i metadati
     */
    private void addVendorExtensionsToResponse(ApiResponse response, WrapperTypeInfo wrapperInfo) {
        try {
            // Converti x-inner-type in snake_case (usa il nome semplice del tipo dati)
            String innerType = wrapperInfo.getDataTypeSimpleName(); // Es: "UserDto"
            String innerTypeSnake = toSnakeCase(innerType);
            
            // Converti x-generic-wrapper in snake_case
            String genericWrapper = "ResponseWrapper"; // Sempre ResponseWrapper
            String genericWrapperSnake = toSnakeCase(genericWrapper);
            
            // ✨ NEW: Usa direttamente il campo wrapperType da WrapperTypeInfo (DTO, LIST, PAGE)
            String genericType = wrapperInfo.wrapperType;
            
            // Aggiungi le vendor extensions alla response
            response.addExtension("x-inner-type", innerTypeSnake);
            response.addExtension("x-generic-wrapper", genericWrapperSnake);
            response.addExtension("x-generic-type", genericType);
            
            // Aggiungi anche x-imports se disponibile (come negli schemi)
            java.util.List<String> imports = new java.util.ArrayList<>();
            imports.add(innerTypeSnake);
            if (innerType.toLowerCase().contains("date") || innerType.toLowerCase().contains("time")) {
                imports.add("date");
            }
            response.addExtension("x-imports", imports);
            
            log.debug("✨ VENDOR_EXTENSIONS: Added to response - x-inner-type: '{}', x-generic-wrapper: '{}', x-generic-type: '{}', x-imports: {}", 
                innerTypeSnake, genericWrapperSnake, genericType, imports);
                
        } catch (Exception e) {
            log.warn("⚠️ VENDOR_EXTENSIONS: Error adding vendor extensions to response: {}", e.getMessage());
        }
    }

    /**
     * ✨ NEW: Aggiunge vendor extensions alle success response con conversione snake_case
     * 
     * @param response ApiResponse da arricchire con vendor extensions
     * @param responseInfo Informazioni della response per estrarre i metadati
     */
    private void addVendorExtensionsToSuccessResponse(ApiResponse response, ResponseInfo responseInfo) {
        try {
            String innerType = extractInnerTypeFromSchemaRef(responseInfo.getWrapperSchemaRef());
            if (innerType != null) {
                // Converti x-inner-type in snake_case
                String innerTypeSnake = toSnakeCase(innerType);
                
                // Converti x-generic-wrapper in snake_case
                String genericWrapperSnake = toSnakeCase("ResponseWrapper");
                
                // ✨ NEW: Estrai il tipo generico dal schema reference
                String genericType = extractGenericTypeFromSchemaRef(responseInfo.getWrapperSchemaRef());
                
                // Aggiungi le vendor extensions alla response
                response.addExtension("x-inner-type", innerTypeSnake);
                response.addExtension("x-generic-wrapper", genericWrapperSnake);
                response.addExtension("x-generic-type", genericType);
                
                // Aggiungi anche x-imports se disponibile (come negli schemi)
                java.util.List<String> imports = new java.util.ArrayList<>();
                imports.add(innerTypeSnake);
                if (innerType.toLowerCase().contains("date") || innerType.toLowerCase().contains("time")) {
                    imports.add("date");
                }
                response.addExtension("x-imports", imports);
                
                log.debug("✨ VENDOR_EXTENSIONS: Added to success response - x-inner-type: '{}', x-generic-wrapper: '{}', x-generic-type: '{}', x-imports: {}", 
                    innerTypeSnake, genericWrapperSnake, genericType, imports);
            }
        } catch (Exception e) {
            log.warn("⚠️ VENDOR_EXTENSIONS: Error adding vendor extensions to success response: {}", e.getMessage());
        }
    }

    /**
     * Estrae il tipo inner dal riferimento schema
     * Da "#/components/schemas/ResponseWrapperUserDto" estrae "UserDto"
     * Da "#/components/schemas/ResponseWrapperListUserDto" estrae "UserDto"  
     * Da "#/components/schemas/ResponseWrapperPageUserDto" estrae "UserDto"
     */
    private String extractInnerTypeFromSchemaRef(String schemaRef) {
        if (schemaRef == null || !schemaRef.startsWith("#/components/schemas/ResponseWrapper")) {
            return null;
        }
        
        String schemaName = schemaRef.substring("#/components/schemas/".length());
        
        // Rimuovi il prefisso ResponseWrapper
        if (schemaName.startsWith("ResponseWrapper")) {
            String remaining = schemaName.substring("ResponseWrapper".length());
            
            // Gestisci i casi List e Page
            if (remaining.startsWith("List")) {
                return remaining.substring("List".length());
            } else if (remaining.startsWith("Page")) {
                return remaining.substring("Page".length());
            } else {
                return remaining;
            }
        }
        
        return null;
    }
    
    /**
     * ✨ NEW: Estrae il tipo generico dal riferimento schema per x-generic-type
     * Da "#/components/schemas/ResponseWrapperListUserDto" estrae "LIST"
     * Da "#/components/schemas/ResponseWrapperPageUserDto" estrae "PAGE"
     * Da "#/components/schemas/ResponseWrapperUserDto" estrae "DTO"
     * Da "#/components/schemas/ResponseWrapperString" estrae "PRIMITIVE"
     */
    private String extractGenericTypeFromSchemaRef(String schemaRef) {
        if (schemaRef == null || !schemaRef.startsWith("#/components/schemas/ResponseWrapper")) {
            return "DTO"; // default
        }
        
        String schemaName = schemaRef.substring("#/components/schemas/".length());
        
        // Rimuovi il prefisso ResponseWrapper
        if (schemaName.startsWith("ResponseWrapper")) {
            String remaining = schemaName.substring("ResponseWrapper".length());
            
            // Analizza il pattern per determinare il tipo generico
            if (remaining.startsWith("List")) {
                return "LIST";
            } else if (remaining.startsWith("Page")) {
                return "PAGE";
            } else if (isPrimitiveType(remaining)) {
                return "PRIMITIVE";
            } else {
                return "DTO";
            }
        }
        
        return "DTO"; // default
    }
    
    /**
     * ✨ NEW: Determina se un tipo è primitivo (String, Integer, Boolean, etc.)
     */
    private boolean isPrimitiveType(String typeName) {
        if (typeName == null) return false;
        
        // Lista dei tipi primitivi comuni
        String lowerType = typeName.toLowerCase();
        return lowerType.equals("string") || 
               lowerType.equals("integer") || 
               lowerType.equals("int") ||
               lowerType.equals("long") ||
               lowerType.equals("double") ||
               lowerType.equals("float") ||
               lowerType.equals("boolean") ||
               lowerType.equals("byte") ||
               lowerType.equals("short") ||
               lowerType.equals("char") ||
               lowerType.equals("uuid") ||
               lowerType.equals("bigdecimal") ||
               lowerType.equals("biginteger") ||
               lowerType.startsWith("localdatetime") ||
               lowerType.startsWith("localdate") ||
               lowerType.startsWith("localtime") ||
               lowerType.startsWith("instant") ||
               lowerType.startsWith("zoneddatetime") ||
               lowerType.startsWith("offsetdatetime");
    }
    
    /**
     * ✨ NEW: Aggiunge vendor extensions statiche per le error responses
     * 
     * @param response ApiResponse di errore da arricchire con vendor extensions
     */
    private void addVendorExtensionsToErrorResponse(ApiResponse response) {
        try {
            // Per gli errori è sempre statico: error_details
            response.addExtension("x-inner-type", "error_details");
            response.addExtension("x-generic-wrapper", "response_wrapper");
            response.addExtension("x-generic-type", "DTO"); // ErrorDetails è sempre un DTO
            
            // x-imports per errori
            java.util.List<String> imports = java.util.List.of("error_details");
            response.addExtension("x-imports", imports);
            
            log.debug("✨ VENDOR_EXTENSIONS: Added to error response - x-inner-type: 'error_details', x-generic-wrapper: 'response_wrapper', x-generic-type: 'DTO'");
            
        } catch (Exception e) {
            log.warn("⚠️ VENDOR_EXTENSIONS: Error adding vendor extensions to error response: {}", e.getMessage());
        }
    }
}
