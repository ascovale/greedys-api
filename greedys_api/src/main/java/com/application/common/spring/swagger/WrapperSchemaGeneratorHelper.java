package com.application.common.spring.swagger;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class per la generazione di schemi wrapper (ResponseWrapper<T>, List<T>, Page<T>)
 * Questa classe si occupa specificatamente di creare gli schemi per i tipi wrapper
 * che avvolgono i dati reali e utilizzano riferimenti ($ref) invece di definizioni inline.
 * 
 * ✅ UPDATED: Aggiunto supporto per schemi consolidati per ridurre ridondanza
 */
@Slf4j
public class WrapperSchemaGeneratorHelper {

    /**
     * ✅ UPDATED: Genera solo schemi metadata necessari 
     * ResponseWrapperError è ora gestito da BaseMetadataSchemaProvider.addCommonSchemas()
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void generateConsolidatedWrapperSchemas(OpenAPI openApi) {
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        // ✅ Genera schemi metadata se non esistono
        generateMetadataSchemas(openApi);
        
        // ✅ ResponseWrapperError è ora generato da BaseMetadataSchemaProvider
        // Non generiamo più schemi generici qui - solo schemi specifici in generateWrapperSchemas()
        
        log.warn("✅ GENERATOR: Metadata schemas verificati - ResponseWrapperError gestito da BaseMetadataSchemaProvider");
    }
    
    /**
     * ✅ NEW: Genera schemi metadata necessari per i wrapper consolidati
     */
    private void generateMetadataSchemas(OpenAPI openApi) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        // Single object metadata
        if (!schemas.containsKey("SingleMetadata")) {
            Schema<?> singleMetadata = new ObjectSchema()
                .title("Metadata for single object responses")
                .addProperty("additional", new ObjectSchema()
                    .description("Additional metadata"));
            schemas.put("SingleMetadata", singleMetadata);
        }
        
        // List metadata
        if (!schemas.containsKey("ListMetadata")) {
            Schema<?> listMetadata = new ObjectSchema()
                .title("Metadata for list responses")
                .addProperty("totalCount", new IntegerSchema()
                    .format("int64")
                    .description("Total number of items in the list")
                    .example(25))
                .addProperty("count", new IntegerSchema()
                    .format("int32")
                    .description("Number of items returned in this response")
                    .example(25))
                .addProperty("filtered", new BooleanSchema()
                    .description("Whether the list is filtered")
                    .example(false))
                .addProperty("filterDescription", new StringSchema()
                    .description("Applied filters description")
                    .example("status=active"))
                .addProperty("additional", new ObjectSchema()
                    .description("Additional metadata"));
            schemas.put("ListMetadata", listMetadata);
        }
        
        // Page metadata
        if (!schemas.containsKey("PageMetadata")) {
            Schema<?> pageMetadata = new ObjectSchema()
                .title("Metadata for paginated responses")
                .addProperty("totalCount", new IntegerSchema()
                    .format("int64")
                    .description("Total number of items across all pages")
                    .example(150))
                .addProperty("count", new IntegerSchema()
                    .format("int32")
                    .description("Number of items in current page")
                    .example(20))
                .addProperty("page", new IntegerSchema()
                    .format("int32")
                    .description("Current page number (0-based)")
                    .example(0))
                .addProperty("size", new IntegerSchema()
                    .format("int32")
                    .description("Number of items per page")
                    .example(20))
                .addProperty("totalPages", new IntegerSchema()
                    .format("int32")
                    .description("Total number of pages")
                    .example(8))
                .addProperty("first", new BooleanSchema()
                    .description("Whether this is the first page")
                    .example(true))
                .addProperty("last", new BooleanSchema()
                    .description("Whether this is the last page")
                    .example(false))
                .addProperty("numberOfElements", new IntegerSchema()
                    .format("int32")
                    .description("Number of elements in current page")
                    .example(20))
                .addProperty("filtered", new BooleanSchema()
                    .description("Whether the list is filtered")
                    .example(false))
                .addProperty("filterDescription", new StringSchema()
                    .description("Applied filters description")
                    .example("status=active"))
                .addProperty("additional", new ObjectSchema()
                    .description("Additional metadata"));
            schemas.put("PageMetadata", pageMetadata);
        }
        
        // Error details schema
        if (!schemas.containsKey("ErrorDetails")) {
            Schema<?> errorDetails = new ObjectSchema()
                .title("Error details for API responses")
                .addProperty("code", new StringSchema()
                    .description("Error code")
                    .example("VALIDATION_ERROR"))
                .addProperty("details", new ObjectSchema()
                    .description("Field-specific errors or additional details"));
            schemas.put("ErrorDetails", errorDetails);
        }
        
        log.debug("✅ GENERATOR: Metadata schemas verificati/creati");
    }

    /**
     * ✅ UPDATED: Genera schemi wrapper specifici FLAT per ogni tipo
     * Crea un schema dedicato per ogni ResponseWrapper<T> per massima compatibilità con OpenAPI Generator
     * Approccio: ResponseWrapperAuthResponseDTO, ResponseWrapperMenuDTO, etc. - NO oneOf, NO allOf
     */
    @SuppressWarnings("rawtypes")
    public void generateWrapperSchemas(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi, WrapperTypeRegistry registry) {
        
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        // Genera schemi metadata comuni
        generateMetadataSchemas(openApi);
        
        log.warn("🔄 GENERATOR: Generazione schemi wrapper SPECIFICI per {} tipi", wrapperTypes.size());
        
        // ✅ STEP 1: Genera uno schema specifico per ogni wrapper type
        for (WrapperTypeInfo wrapperInfo : wrapperTypes) {
            String dataClassName = wrapperInfo.dataClassName;
            String simpleClassName = dataClassName.substring(dataClassName.lastIndexOf('.') + 1);
            String wrapperType = wrapperInfo.wrapperType;
            
            // Genera il nome dello schema specifico
            String schemaName = generateSpecificSchemaName(simpleClassName, wrapperType);
            
            // 🎯 TRACE SPECIFICO PER AuthResponseDTO
            if (dataClassName.contains("AuthResponseDTO")) {
                log.warn("🎯 FASE4-AuthResponseDTO: PROCESSING wrapperInfo! dataClass={}, wrapperType={}, schemaName={}", 
                    dataClassName, wrapperType, schemaName);
            }
            
            // Crea lo schema specifico basato sul tipo wrapper
            Schema<?> wrapperSchema = createSpecificWrapperSchema(simpleClassName, wrapperType, schemaName);
            
            // Registra lo schema
            schemas.put(schemaName, wrapperSchema);
            registry.registerWrapperSchema(dataClassName, wrapperType, schemaName);
            
            if (dataClassName.contains("AuthResponseDTO")) {
                log.warn("🎯 FASE4-AuthResponseDTO: SCHEMA SPECIFICO GENERATO! schema={}", schemaName);
            }
            
            log.debug("✅ GENERATED SPECIFIC: {} -> {}", dataClassName, schemaName);
        }
        
        log.warn("✅ GENERATOR: Schemi specifici completati - {} tipi → {} schemi dedicati", 
            wrapperTypes.size(), wrapperTypes.size());
    }
    
    /**
     * ✅ NEW: Genera il nome dello schema specifico basato sul tipo di dato e wrapper
     */
    private String generateSpecificSchemaName(String simpleClassName, String wrapperType) {
        return switch (wrapperType) {
            case "DTO" -> "ResponseWrapper" + simpleClassName;
            case "LIST" -> "ResponseWrapperList" + simpleClassName;
            case "PAGE" -> "ResponseWrapperPage" + simpleClassName;
            case "VOID" -> "ResponseWrapperVoid";
            default -> {
                log.warn("⚠️ Tipo wrapper sconosciuto: {}, uso DTO", wrapperType);
                yield "ResponseWrapper" + simpleClassName;
            }
        };
    }
    
    /**
     * ✅ NEW: Crea uno schema wrapper specifico flat per massima compatibilità OpenAPI Generator
     */
    private Schema<?> createSpecificWrapperSchema(String simpleClassName, String wrapperType, String schemaName) {
        return switch (wrapperType) {
            case "DTO" -> createResponseWrapperSchema(simpleClassName, schemaName);
            case "LIST" -> createResponseWrapperListSchema(simpleClassName, schemaName);
            case "PAGE" -> createResponseWrapperPageSchema(simpleClassName, schemaName);
            case "VOID" -> createResponseWrapperVoidSchema(schemaName);
            default -> {
                log.warn("⚠️ Tipo wrapper sconosciuto: {}, uso DTO", wrapperType);
                yield createResponseWrapperSchema(simpleClassName, schemaName);
            }
        };
    }
    
    /**
     * ✅ FIXED: Crea schema specifico per ResponseWrapper<T>
     * Gestisce correttamente i tipi primitivi usando type invece di $ref
     */
    @SuppressWarnings("unchecked")
    private Schema<?> createResponseWrapperSchema(String simpleClassName, String schemaName) {
        return new ObjectSchema()
            .title("Response wrapper for " + simpleClassName)
            .description("API response containing " + simpleClassName + " data or error details")
            .addProperty("success", new BooleanSchema()
                .description("Indicates if the operation was successful"))
            .addProperty("data", createDataSchemaForType(simpleClassName))
            .addProperty("message", new StringSchema()
                .description("Response message"))
            .addProperty("timestamp", new StringSchema()
                .format("date-time")
                .description("Response timestamp"))
            .addProperty("error", new Schema<>()
                .$ref("#/components/schemas/ErrorDetails")
                .description("Error details when success=false"))
            .addProperty("metadata", new Schema<>()
                .$ref("#/components/schemas/SingleMetadata")
                .description("Response metadata"))
            .required(Arrays.asList("success", "timestamp"));
    }
    
    /**
     * ✅ FIXED: Crea schema specifico per ResponseWrapper<List<T>>
     * Gestisce correttamente i tipi primitivi usando type invece di $ref
     */
    @SuppressWarnings("unchecked")
    private Schema<?> createResponseWrapperListSchema(String simpleClassName, String schemaName) {
        return new ObjectSchema()
            .title("Response wrapper for List<" + simpleClassName + ">")
            .description("API response containing list of " + simpleClassName + " or error details")
            .addProperty("success", new BooleanSchema()
                .description("Indicates if the operation was successful"))
            .addProperty("data", new ArraySchema()
                .description("List of " + simpleClassName + " when success=true")
                .items(createDataSchemaForType(simpleClassName)))
            .addProperty("message", new StringSchema()
                .description("Response message"))
            .addProperty("timestamp", new StringSchema()
                .format("date-time")
                .description("Response timestamp"))
            .addProperty("error", new Schema<>()
                .$ref("#/components/schemas/ErrorDetails")
                .description("Error details when success=false"))
            .addProperty("metadata", new Schema<>()
                .$ref("#/components/schemas/ListMetadata")
                .description("List metadata"))
            .required(Arrays.asList("success", "timestamp"));
    }
    
    /**
     * ✅ FIXED: Crea schema specifico per ResponseWrapper<Page<T>>
     * Gestisce correttamente i tipi primitivi usando type invece di $ref
     */
    @SuppressWarnings("unchecked")
    private Schema<?> createResponseWrapperPageSchema(String simpleClassName, String schemaName) {
        Schema<?> pageDataSchema = new ObjectSchema()
            .description("Page data with content and pagination info")
            .addProperty("content", new ArraySchema()
                .description("Page content items")
                .items(createDataSchemaForType(simpleClassName)))
            .addProperty("totalElements", new IntegerSchema().format("int64"))
            .addProperty("totalPages", new IntegerSchema().format("int32"))
            .addProperty("size", new IntegerSchema().format("int32"))
            .addProperty("number", new IntegerSchema().format("int32"))
            .addProperty("numberOfElements", new IntegerSchema().format("int32"))
            .addProperty("first", new BooleanSchema())
            .addProperty("last", new BooleanSchema())
            .addProperty("empty", new BooleanSchema());
        
        return new ObjectSchema()
            .title("Response wrapper for Page<" + simpleClassName + ">")
            .description("API response containing paginated " + simpleClassName + " data or error details")
            .addProperty("success", new BooleanSchema()
                .description("Indicates if the operation was successful"))
            .addProperty("data", pageDataSchema)
            .addProperty("message", new StringSchema()
                .description("Response message"))
            .addProperty("timestamp", new StringSchema()
                .format("date-time")
                .description("Response timestamp"))
            .addProperty("error", new Schema<>()
                .$ref("#/components/schemas/ErrorDetails")
                .description("Error details when success=false"))
            .addProperty("metadata", new Schema<>()
                .$ref("#/components/schemas/PageMetadata")
                .description("Page metadata"))
            .required(Arrays.asList("success", "timestamp"));
    }
    
    /**
     * ✅ NEW: Crea schema specifico per ResponseWrapper<Void>
     */
    @SuppressWarnings("unchecked")
    private Schema<?> createResponseWrapperVoidSchema(String schemaName) {
        return new ObjectSchema()
            .title("Response wrapper for void operations")
            .description("API response for operations that don't return data")
            .addProperty("success", new BooleanSchema()
                .description("Indicates if the operation was successful"))
            .addProperty("message", new StringSchema()
                .description("Response message"))
            .addProperty("timestamp", new StringSchema()
                .format("date-time")
                .description("Response timestamp"))
            .addProperty("error", new Schema<>()
                .$ref("#/components/schemas/ErrorDetails")
                .description("Error details when success=false"))
            .addProperty("metadata", new Schema<>()
                .$ref("#/components/schemas/SingleMetadata")
                .description("Response metadata"))
            .required(Arrays.asList("success", "timestamp"));
    }
    
    /**
     * ✅ NEW: Verifica se un tipo è primitivo (non necessita di $ref)
     */
    private boolean isPrimitiveType(String simpleClassName) {
        return switch (simpleClassName.toLowerCase()) {
            case "string" -> true;
            case "long" -> true;
            case "integer" -> true;
            case "boolean" -> true;
            case "localdate" -> true;
            case "localdatetime" -> true;
            default -> false;
        };
    }
    
    /**
     * ✅ NEW: Crea schema per il campo data basato sul tipo
     * Se è un tipo primitivo usa type direttamente, altrimenti usa $ref
     */
    @SuppressWarnings("unchecked")
    private Schema<?> createDataSchemaForType(String simpleClassName) {
        if (isPrimitiveType(simpleClassName)) {
            // Per tipi primitivi, usa type direttamente
            return switch (simpleClassName.toLowerCase()) {
                case "string" -> new StringSchema()
                    .description("Response data when success=true");
                case "long" -> new IntegerSchema()
                    .format("int64")
                    .description("Response data when success=true");
                case "integer" -> new IntegerSchema()
                    .format("int32")
                    .description("Response data when success=true");
                case "boolean" -> new BooleanSchema()
                    .description("Response data when success=true");
                case "localdate" -> new StringSchema()
                    .format("date")
                    .description("Response data when success=true");
                case "localdatetime" -> new StringSchema()
                    .format("date-time")
                    .description("Response data when success=true");
                default -> {
                    log.warn("⚠️ Tipo primitivo non gestito: {}, uso String", simpleClassName);
                    yield new StringSchema().description("Response data when success=true");
                }
            };
        } else {
            // Per tipi complessi, usa $ref
            return new Schema<>()
                .$ref("#/components/schemas/" + simpleClassName)
                .description("Response data when success=true");
        }
    }

}