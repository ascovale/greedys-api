package com.application.common.spring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.Components;
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

        // Assicurati che components e schemas esistano
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new HashMap<>());
        }

        // Raccogli tutti i className referenziati negli x-wrapper-type
        Set<String> referencedClasses = collectReferencedClasses(openApi);
        
        // Crea gli schemi mancanti usando le annotazioni delle classi
        createMissingSchemasFromAnnotations(referencedClasses, openApi);

        // Process each path and operation
        openApi.getPaths().forEach((path, pathItem) -> {
            processPathItem(pathItem, openApi);
        });
    }

    private Set<String> collectReferencedClasses(OpenAPI openApi) {
        Set<String> classes = new HashSet<>();
        
        openApi.getPaths().forEach((path, pathItem) -> {
            if (pathItem.getGet() != null) collectFromOperation(pathItem.getGet(), classes);
            if (pathItem.getPost() != null) collectFromOperation(pathItem.getPost(), classes);
            if (pathItem.getPut() != null) collectFromOperation(pathItem.getPut(), classes);
            if (pathItem.getDelete() != null) collectFromOperation(pathItem.getDelete(), classes);
            if (pathItem.getPatch() != null) collectFromOperation(pathItem.getPatch(), classes);
        });
        
        return classes;
    }

    private void collectFromOperation(Operation operation, Set<String> classes) {
        if (operation.getExtensions() != null) {
            Object wrapperType = operation.getExtensions().get("x-wrapper-type");
            if (wrapperType instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> wrapperMap = (Map<String, Object>) wrapperType;
                String dataClass = (String) wrapperMap.get("dataClass");
                if (dataClass != null) {
                    classes.add(dataClass);
                }
            }
        }
    }

    private void createMissingSchemasFromAnnotations(Set<String> referencedClasses, OpenAPI openApi) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        for (String fullClassName : referencedClasses) {
            String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
            
            if (!schemas.containsKey(simpleClassName)) {
                Schema<?> newSchema = createSchemaFromClassAnnotations(fullClassName);
                if (newSchema != null) {
                    schemas.put(simpleClassName, newSchema);
                    System.out.println("Created schema from annotations: " + simpleClassName + " for class: " + fullClassName);
                } else {
                    // Fallback per tipi primitivi
                    Schema<?> fallbackSchema = createPrimitiveSchema(simpleClassName);
                    if (fallbackSchema != null) {
                        schemas.put(simpleClassName, fallbackSchema);
                        System.out.println("Created primitive schema: " + simpleClassName);
                    }
                }
            }
        }
    }

    private Schema<?> createSchemaFromClassAnnotations(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            
            // Verifica se la classe ha l'annotazione @Schema
            io.swagger.v3.oas.annotations.media.Schema schemaAnnotation = 
                clazz.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            
            if (schemaAnnotation != null) {
                ObjectSchema schema = new ObjectSchema();
                
                // Usa il nome dall'annotazione se specificato, altrimenti il nome della classe
                String schemaName = !schemaAnnotation.name().isEmpty() ? 
                    schemaAnnotation.name() : clazz.getSimpleName();
                schema.setName(schemaName);
                
                // Usa la descrizione dall'annotazione se specificata
                if (!schemaAnnotation.description().isEmpty()) {
                    schema.setDescription(schemaAnnotation.description());
                }
                
                // Analizza i campi della classe per le proprietà
                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    io.swagger.v3.oas.annotations.media.Schema fieldSchema = 
                        field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                    
                    if (fieldSchema != null) {
                        Schema<?> propertySchema = createSchemaFromFieldAnnotation(field, fieldSchema);
                        schema.addProperty(field.getName(), propertySchema);
                    } else {
                        // Campo senza annotazione, crea schema di base dal tipo
                        Schema<?> propertySchema = createSchemaFromFieldType(field);
                        schema.addProperty(field.getName(), propertySchema);
                    }
                }
                
                return schema;
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + fullClassName + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating schema for " + fullClassName + ": " + e.getMessage());
        }
        
        return null;
    }

    private Schema<?> createSchemaFromFieldAnnotation(java.lang.reflect.Field field, 
            io.swagger.v3.oas.annotations.media.Schema fieldSchema) {
        Schema<?> schema = createSchemaFromFieldType(field);
        
        // Applica le proprietà dall'annotazione
        if (!fieldSchema.description().isEmpty()) {
            schema.setDescription(fieldSchema.description());
        }
        if (!fieldSchema.example().isEmpty()) {
            schema.setExample(fieldSchema.example());
        }
        
        return schema;
    }

    private Schema<?> createSchemaFromFieldType(java.lang.reflect.Field field) {
        Class<?> fieldType = field.getType();
        String typeName = fieldType.getSimpleName();
        
        // Gestisce i tipi primitivi e comuni
        switch (typeName) {
            case "String":
                return new Schema<>().type("string");
            case "Long":
            case "long":
                return new Schema<>().type("integer").format("int64");
            case "Integer":
            case "int":
                return new Schema<>().type("integer").format("int32");
            case "Boolean":
            case "boolean":
                return new Schema<>().type("boolean");
            case "Double":
            case "double":
                return new Schema<>().type("number").format("double");
            case "Float":
            case "float":
                return new Schema<>().type("number").format("float");
            case "LocalDate":
                return new Schema<>().type("string").format("date");
            case "LocalDateTime":
                return new Schema<>().type("string").format("date-time");
            case "BigDecimal":
                return new Schema<>().type("number");
            default:
                // Per tipi complessi, crea un riferimento
                if (fieldType.getPackage() != null && 
                    fieldType.getPackage().getName().startsWith("com.application")) {
                    Schema<?> refSchema = new Schema<>();
                    refSchema.set$ref("#/components/schemas/" + typeName);
                    return refSchema;
                } else {
                    // Tipo sconosciuto, usa object generico
                    return new Schema<>().type("object");
                }
        }
    }

    private Schema<?> createPrimitiveSchema(String simpleClassName) {
        switch (simpleClassName) {
            case "String":
                return new Schema<>().type("string");
            case "Long":
                return new Schema<>().type("integer").format("int64");
            case "Integer":
                return new Schema<>().type("integer").format("int32");
            case "Boolean":
                return new Schema<>().type("boolean");
            case "Double":
                return new Schema<>().type("number").format("double");
            case "Float":
                return new Schema<>().type("number").format("float");
            case "LocalDate":
                return new Schema<>().type("string").format("date");
            case "LocalDateTime":
                return new Schema<>().type("string").format("date-time");
            case "BigDecimal":
                return new Schema<>().type("number");
            default:
                return null;
        }
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
        
        // Always use schema reference - the schema should be defined in components
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
