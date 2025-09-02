package com.application.common.spring.swagger.generator;

import java.util.Arrays;
import java.util.List;

import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.metadata.SchemaMetadata;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Aggiornatore per responses delle operazioni
 */
@UtilityClass
@Slf4j
public class OperationResponseUpdater {
    
    public static void updateOperationResponses(Operation operation, SchemaMetadata metadata) {
        updateSuccessResponse(operation, metadata);
        updateCreatedResponse(operation, metadata);
        updateErrorResponse(operation, metadata);
        updateAllErrorResponses(operation, metadata);
    }
    
    /**
     * Aggiorna le responses per tutti gli status codes dal MethodAnalyzer
     */
    public static void updateOperationResponses(Operation operation, OperationDataMetadata operationData) {
        // Crea responses per TUTTI gli status codes del MethodAnalyzer
        for (String statusCode : operationData.getStatusCodes()) {
            if (statusCode.equals("200")) {
                updateSuccessResponseFromOperationData(operation, operationData);
            } else if (statusCode.equals("201")) {
                updateCreatedResponseFromOperationData(operation, operationData);
            } else {
                // Tutti gli altri status codes sono errori (usano ResponseWrapperErrorDetails)
                createErrorResponse(operation, statusCode);
            }
        }
    }
    
    /**
     * Crea una error response per uno status code specifico
     */
    private static void createErrorResponse(Operation operation, String statusCode) {
        ApiResponse errorResponse = new ApiResponse()
            .description(getErrorDescription(statusCode))
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/ResponseWrapperErrorDetails"))));
        
        operation.getResponses().addApiResponse(statusCode, errorResponse);
        log.debug("Created {} error response", statusCode);
    }
    
    public static void fixErrorResponseReferences(io.swagger.v3.oas.models.responses.ApiResponses responses) {
        List<String> errorCodes = Arrays.asList("400", "401", "403", "404", "409", "500");
        
        for (String errorCode : errorCodes) {
            ApiResponse existingResponse = responses.get(errorCode);
            if (existingResponse != null) {
                String description = existingResponse.getDescription();
                
                ApiResponse updatedErrorResponse = new ApiResponse()
                    .description(description != null ? description : "Error response")
                    .content(new Content()
                        .addMediaType("*/*", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ResponseWrapperErrorDetails"))));
                
                responses.addApiResponse(errorCode, updatedErrorResponse);
            }
        }
    }
    
    private static void updateSuccessResponse(Operation operation, SchemaMetadata metadata) {
        ApiResponse successResponse = new ApiResponse()
            .description("Successful operation")
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>().$ref(metadata.getSuccessSchemaRef()))));
        
        operation.getResponses().put("200", successResponse);
        log.debug("Updated 200 response for operation: {} with schema: {}", 
            operation.getOperationId(), metadata.getSuccessSchemaRef());
    }
    
    private static void updateCreatedResponse(Operation operation, SchemaMetadata metadata) {
        if (operation.getResponses().containsKey("201")) {
            ApiResponse createdResponse = new ApiResponse()
                .description("Resource created successfully")
                .content(new Content()
                    .addMediaType("*/*", new MediaType()
                        .schema(new Schema<>().$ref(metadata.getSuccessSchemaRef()))));
            
            operation.getResponses().addApiResponse("201", createdResponse);
        }
    }
    
    private static void updateErrorResponse(Operation operation, SchemaMetadata metadata) {
        ApiResponse errorResponse = new ApiResponse()
            .description("Error response")
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>().$ref(metadata.getErrorSchemaRef()))));
        
        operation.getResponses().addApiResponse("400", errorResponse);
    }
    
    private static void updateAllErrorResponses(Operation operation, SchemaMetadata metadata) {
        String[] errorCodes = {"400", "401", "403", "404", "405", "409", "500"};
        
        for (String errorCode : errorCodes) {
            ApiResponse existingResponse = operation.getResponses().get(errorCode);
            if (existingResponse != null) {
                String description = existingResponse.getDescription();
                
                ApiResponse updatedErrorResponse = new ApiResponse()
                    .description(description)
                    .content(new Content()
                        .addMediaType("*/*", new MediaType()
                            .schema(new Schema<>().$ref(metadata.getErrorSchemaRef()))));
                
                operation.getResponses().addApiResponse(errorCode, updatedErrorResponse);
            }
        }
    }
    
    // === METODI PER OperationDataMetadata (nuovo sistema) ===
    
    private static void updateSuccessResponseFromOperationData(Operation operation, OperationDataMetadata operationData) {
        // Genera il nome schema concreto per la response 200
        String schemaName = generateConcreteSchemaName(operationData);
        
        ApiResponse successResponse = new ApiResponse()
            .description("Operation completed successfully")
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/" + schemaName))));
        
        operation.getResponses().addApiResponse("200", successResponse);
        log.debug("Updated 200 response for operation {}: {}", operationData.getOperationId(), schemaName);
    }
    
    private static void updateCreatedResponseFromOperationData(Operation operation, OperationDataMetadata operationData) {
        // Genera il nome schema concreto per la response 201
        String schemaName = generateConcreteSchemaName(operationData);
        
        ApiResponse createdResponse = new ApiResponse()
            .description("Resource created successfully")
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/" + schemaName))));
        
        operation.getResponses().addApiResponse("201", createdResponse);
        log.debug("Updated 201 response for operation {}: {}", operationData.getOperationId(), schemaName);
    }
    
    /**
     * Genera il nome concreto dello schema wrapper
     * Es: ResponseWrapperReservationDTO, ResponseWrapperListCustomerDTO, etc.
     */
    private static String generateConcreteSchemaName(OperationDataMetadata operationData) {
        String dataClassName = operationData.getDataClassName();
        String simpleClassName = extractSimpleClassName(dataClassName);
        
        // Usa la categoria wrapper per generare il nome corretto
        return operationData.getWrapperCategory().generateSchemaName(simpleClassName);
    }
    
    /**
     * Estrae il nome semplice della classe gestendo i primitivi
     */
    private static String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) return "String";
        
        // Gestisce i primitivi
        if (isPrimitiveClass(fullClassName)) {
            return mapPrimitiveToSchemaName(fullClassName);
        }
        
        // Estrae il nome semplice per le classi non primitive
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
    
    /**
     * Verifica se è una classe primitiva, wrapper o data type
     */
    private static boolean isPrimitiveClass(String className) {
        return className.equals("java.lang.String") || 
               className.equals("java.lang.Integer") ||
               className.equals("java.lang.Long") ||
               className.equals("java.lang.Boolean") ||
               className.equals("java.lang.Double") ||
               className.equals("java.lang.Float") ||
               className.equals("java.lang.Object") ||
               className.equals("java.lang.Void") ||
               className.equals("String") ||
               className.equals("Integer") ||
               className.equals("Long") ||
               className.equals("Boolean") ||
               className.equals("Double") ||
               className.equals("Float") ||
               className.equals("Object") ||
               className.equals("Void") ||
               // Tipi data
               className.equals("java.util.Date") ||
               className.equals("java.time.LocalDate") ||
               className.equals("java.time.LocalDateTime") ||
               className.equals("java.time.LocalTime") ||
               className.equals("java.time.OffsetDateTime") ||
               className.equals("java.time.ZonedDateTime") ||
               className.equals("java.time.Instant") ||
               className.equals("Date") ||
               className.equals("LocalDate") ||
               className.equals("LocalDateTime") ||
               className.equals("LocalTime") ||
               className.equals("OffsetDateTime") ||
               className.equals("ZonedDateTime") ||
               className.equals("Instant");
    }
    
    /**
     * Mappa le classi primitive e data types ai nomi schema
     */
    private static String mapPrimitiveToSchemaName(String className) {
        return switch (className) {
            case "java.lang.String", "String" -> "String";
            case "java.lang.Integer", "Integer" -> "Integer";
            case "java.lang.Long", "Long" -> "Long";
            case "java.lang.Boolean", "Boolean" -> "Boolean";
            case "java.lang.Double", "Double" -> "Double";
            case "java.lang.Float", "Float" -> "Float";
            case "java.lang.Object", "Object" -> "Object";
            case "java.lang.Void", "Void" -> "String"; // Void -> String per compatibilità
            // Tipi data -> tutti mappati a String per schema wrapper
            case "java.util.Date", "Date",
                 "java.time.LocalDate", "LocalDate",
                 "java.time.LocalDateTime", "LocalDateTime",
                 "java.time.LocalTime", "LocalTime",
                 "java.time.OffsetDateTime", "OffsetDateTime",
                 "java.time.ZonedDateTime", "ZonedDateTime",
                 "java.time.Instant", "Instant" -> "String";
            default -> "String";
        };
    }
    
    /**
     * Ottiene la descrizione per tutti gli status codes di errore
     */
    private static String getErrorDescription(String statusCode) {
        return switch (statusCode) {
            case "400" -> "Bad Request - Invalid input parameters";
            case "401" -> "Unauthorized - Authentication required";
            case "403" -> "Forbidden - Access denied";
            case "404" -> "Not Found - Resource not found";
            case "409" -> "Conflict - Resource conflict";
            case "500" -> "Internal Server Error";
            default -> "Error - Status " + statusCode;
        };
    }
}
