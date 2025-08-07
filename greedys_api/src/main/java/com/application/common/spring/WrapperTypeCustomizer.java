package com.application.common.spring;

import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * OpenAPI customizer that processes @WrapperType annotations and generates
 * appropriate schemas for ResponseWrapper, ListResponseWrapper, and PageResponseWrapper
 */
@Component
public class WrapperTypeCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }

        // Process each path and operation
        openApi.getPaths().forEach((path, pathItem) -> {
            processPathItem(pathItem, openApi);
        });
    }

    private void processPathItem(PathItem pathItem, OpenAPI openApi) {
        processOperation(pathItem.getGet(), openApi);
        processOperation(pathItem.getPost(), openApi);
        processOperation(pathItem.getPut(), openApi);
        processOperation(pathItem.getDelete(), openApi);
        processOperation(pathItem.getPatch(), openApi);
    }

    private void processOperation(Operation operation, OpenAPI openApi) {
        if (operation == null || operation.getExtensions() == null) {
            return;
        }

        // Look for @WrapperType annotation data in extensions
        Object wrapperTypeData = operation.getExtensions().get("x-wrapper-type");
        if (wrapperTypeData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> wrapperInfo = (Map<String, Object>) wrapperTypeData;
            
            String dataClassName = (String) wrapperInfo.get("dataClass");
            String wrapperType = (String) wrapperInfo.get("type");
            String description = (String) wrapperInfo.getOrDefault("description", "Successful operation");
            String responseCode = (String) wrapperInfo.getOrDefault("responseCode", "200");

            if (dataClassName != null && wrapperType != null) {
                Schema<?> responseSchema = createWrapperSchema(dataClassName, wrapperType, openApi);
                updateOperationResponse(operation, responseSchema, description, responseCode);
            }
        }
    }

    private Schema<?> createWrapperSchema(String dataClassName, String wrapperType, OpenAPI openApi) {
        Schema<?> dataSchema = createDataSchema(dataClassName);
        
        switch (wrapperType) {
            case "DTO":
                return createResponseWrapperSchema(dataSchema);
            case "LIST":
                return createListResponseWrapperSchema(dataSchema);
            case "PAGE":
                return createPageResponseWrapperSchema(dataSchema);
            default:
                return createResponseWrapperSchema(dataSchema);
        }
    }

    private Schema<?> createDataSchema(String className) {
        // Extract simple class name for schema reference
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/" + simpleClassName);
        return schema;
    }

    private Schema<?> createResponseWrapperSchema(Schema<?> dataSchema) {
        ObjectSchema wrapper = new ObjectSchema();
        wrapper.addProperty("success", new Schema<>().type("boolean").description("Indicates if the operation was successful"));
        wrapper.addProperty("message", new Schema<>().type("string").description("Response message"));
        wrapper.addProperty("data", dataSchema);
        wrapper.addProperty("errors", new ArraySchema().items(new Schema<>().type("string")));
        return wrapper;
    }

    private Schema<?> createListResponseWrapperSchema(Schema<?> dataSchema) {
        ObjectSchema wrapper = new ObjectSchema();
        wrapper.addProperty("success", new Schema<>().type("boolean").description("Indicates if the operation was successful"));
        wrapper.addProperty("message", new Schema<>().type("string").description("Response message"));
        wrapper.addProperty("data", new ArraySchema().items(dataSchema));
        wrapper.addProperty("errors", new ArraySchema().items(new Schema<>().type("string")));
        return wrapper;
    }

    private Schema<?> createPageResponseWrapperSchema(Schema<?> dataSchema) {
        ObjectSchema wrapper = new ObjectSchema();
        wrapper.addProperty("success", new Schema<>().type("boolean").description("Indicates if the operation was successful"));
        wrapper.addProperty("message", new Schema<>().type("string").description("Response message"));
        
        // Create page data structure
        ObjectSchema pageData = new ObjectSchema();
        pageData.addProperty("content", new ArraySchema().items(dataSchema));
        pageData.addProperty("totalElements", new Schema<>().type("integer").format("int64"));
        pageData.addProperty("totalPages", new Schema<>().type("integer").format("int32"));
        pageData.addProperty("size", new Schema<>().type("integer").format("int32"));
        pageData.addProperty("number", new Schema<>().type("integer").format("int32"));
        pageData.addProperty("first", new Schema<>().type("boolean"));
        pageData.addProperty("last", new Schema<>().type("boolean"));
        pageData.addProperty("numberOfElements", new Schema<>().type("integer").format("int32"));
        pageData.addProperty("empty", new Schema<>().type("boolean"));
        
        wrapper.addProperty("data", pageData);
        wrapper.addProperty("errors", new ArraySchema().items(new Schema<>().type("string")));
        return wrapper;
    }

    private void updateOperationResponse(Operation operation, Schema<?> schema, String description, String responseCode) {
        ApiResponse response = new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", 
                                new MediaType().schema(schema)));
        
        if (operation.getResponses() == null) {
            operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses());
        }
        
        operation.getResponses().addApiResponse(responseCode, response);
    }
}
