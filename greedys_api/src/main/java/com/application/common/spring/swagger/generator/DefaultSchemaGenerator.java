package com.application.common.spring.swagger.generator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Generatore per schemi base e comuni (metadata, errori)
 */
@UtilityClass
@Slf4j
public class DefaultSchemaGenerator {
    
    private static final Schema<?> TIMESTAMP_PROPERTY = new Schema<>().type("string").format("date-time");
    private static final Schema<?> ADDITIONAL_PROPERTY = new Schema<>().type("object").description("Additional metadata");
    private static final Schema<?> MESSAGE_PROPERTY = new Schema<>().type("string");
    private static final Schema<?> INTEGER_PROPERTY = new Schema<>().type("integer");
    private static final Schema<?> BOOLEAN_PROPERTY = new Schema<>().type("boolean");
    
    public static void generateCommonSchemas(OpenAPI openApi) {
        initializeComponents(openApi);
        generateErrorSchema(openApi);
        generateMetadataSchemas(openApi);
        generateErrorWrapperSchema(openApi);
    }
    
    private static void initializeComponents(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.components(new io.swagger.v3.oas.models.Components());
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new java.util.HashMap<>());
        }
    }
    
    private static void generateErrorSchema(OpenAPI openApi) {
        Schema<?> errorSchema = new Schema<>()
            .type("object")
            .description("Standard error response")
            .addProperty("success", BOOLEAN_PROPERTY.example(false))
            .addProperty("message", MESSAGE_PROPERTY.example("Error message"))
            .addProperty("errorCode", MESSAGE_PROPERTY.example("ERROR_CODE"))
            .addProperty("timestamp", TIMESTAMP_PROPERTY);
        
        openApi.getComponents().getSchemas().put("ErrorResponse", errorSchema);
    }
    
    private static void generateMetadataSchemas(OpenAPI openApi) {
        generateBaseMetadata(openApi);
        generateSingleMetadata(openApi);
        generateListMetadata(openApi);
        generatePageMetadata(openApi);
    }
    
    private static void generateBaseMetadata(OpenAPI openApi) {
        Schema<?> schema = new Schema<>()
            .type("object")
            .description("Base metadata for API responses")
            .addProperty("additional", ADDITIONAL_PROPERTY);
        openApi.getComponents().getSchemas().put("BaseMetadata", schema);
    }
    
    private static void generateSingleMetadata(OpenAPI openApi) {
        Schema<?> schema = new Schema<>()
            .type("object")
            .description("Metadata for single object responses")
            .addProperty("additional", ADDITIONAL_PROPERTY);
        openApi.getComponents().getSchemas().put("SingleMetadata", schema);
    }
    
    private static void generateListMetadata(OpenAPI openApi) {
        Schema<?> schema = new Schema<>()
            .type("object")
            .description("Metadata for list responses")
            .addProperty("count", INTEGER_PROPERTY.description("Number of items in the list"))
            .addProperty("filterDescription", MESSAGE_PROPERTY.description("Description of applied filters"))
            .addProperty("additional", ADDITIONAL_PROPERTY);
        openApi.getComponents().getSchemas().put("ListMetadata", schema);
    }
    
    private static void generatePageMetadata(OpenAPI openApi) {
        Schema<?> schema = new Schema<>()
            .type("object")
            .description("Metadata for paginated responses")
            .addProperty("page", INTEGER_PROPERTY.description("Current page number"))
            .addProperty("size", INTEGER_PROPERTY.description("Page size"))
            .addProperty("totalElements", INTEGER_PROPERTY.format("int64").description("Total number of elements"))
            .addProperty("totalPages", INTEGER_PROPERTY.description("Total number of pages"))
            .addProperty("hasNext", BOOLEAN_PROPERTY.description("Whether there is a next page"))
            .addProperty("hasPrevious", BOOLEAN_PROPERTY.description("Whether there is a previous page"))
            .addProperty("additional", ADDITIONAL_PROPERTY);
        openApi.getComponents().getSchemas().put("PageMetadata", schema);
    }
    
    private static void generateErrorWrapperSchema(OpenAPI openApi) {
        Schema<?> errorWrapperSchema = new Schema<>()
            .type("object")
            .title("API Error Response")
            .description("Standard API response wrapper for HTTP 4xx and 5xx error cases")
            .addProperty("data", new Schema<>().description("Error details").type("object"))
            .addProperty("message", MESSAGE_PROPERTY.description("Human-readable error message").example("Operation failed"))
            .addProperty("metadata", new Schema<>().$ref("#/components/schemas/SingleMetadata").description("Additional error context"))
            .addProperty("timestamp", TIMESTAMP_PROPERTY.description("Error occurrence timestamp"));
                
        errorWrapperSchema.setRequired(java.util.Arrays.asList("timestamp"));
        errorWrapperSchema.addExtension("x-response-wrapper", true);
        
        openApi.getComponents().getSchemas().put("ResponseWrapperErrorDetails", errorWrapperSchema);
        log.debug("Generated ResponseWrapperErrorDetails schema for error responses");
    }
}
