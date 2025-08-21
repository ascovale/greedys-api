package com.application.common.spring.swagger;

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
 * OpenAPI customizer that processes wrapper type extensions and generates
 * appropriate schemas for ResponseWrapper with different metadata types
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

        // Aggiungi sempre gli schemi per i metadata e altri tipi di supporto
        addRequiredSchemas(openApi);

        // Raccogli tutti i className referenziati negli x-wrapper-type
        Set<String> referencedClasses = collectReferencedClasses(openApi);
        
        // Crea gli schemi mancanti usando le annotazioni delle classi
        createMissingSchemasFromAnnotations(referencedClasses, openApi);

        // Process each path and operation
        openApi.getPaths().forEach((path, pathItem) -> {
            processPathItem(pathItem, openApi);
        });
    }
    
    private void addRequiredSchemas(OpenAPI openApi) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        // BaseMetadata schema
        if (!schemas.containsKey("BaseMetadata")) {
            ObjectSchema baseMetadata = new ObjectSchema();
            baseMetadata.setName("BaseMetadata");
            baseMetadata.setDescription("Base metadata for API responses");
            baseMetadata.addProperty("dataType", new Schema<>().type("string").description("Type of data returned"));
            baseMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            schemas.put("BaseMetadata", baseMetadata);
        }
        
        // SingleMetadata schema
        if (!schemas.containsKey("SingleMetadata")) {
            ObjectSchema singleMetadata = new ObjectSchema();
            singleMetadata.setName("SingleMetadata");
            singleMetadata.setDescription("Metadata for single object responses");
            singleMetadata.addProperty("dataType", new Schema<>().type("string").example("single"));
            singleMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            schemas.put("SingleMetadata", singleMetadata);
        }
        
        // ListMetadata schema
        if (!schemas.containsKey("ListMetadata")) {
            ObjectSchema listMetadata = new ObjectSchema();
            listMetadata.setName("ListMetadata");
            listMetadata.setDescription("Metadata for list responses");
            listMetadata.addProperty("dataType", new Schema<>().type("string").example("list"));
            listMetadata.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items"));
            listMetadata.addProperty("count", new Schema<>().type("integer").description("Number of items in response"));
            listMetadata.addProperty("filtered", new Schema<>().type("boolean").description("Whether the list is filtered"));
            listMetadata.addProperty("filterDescription", new Schema<>().type("string").description("Applied filters description"));
            listMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            schemas.put("ListMetadata", listMetadata);
        }
        
        // PageMetadata schema
        if (!schemas.containsKey("PageMetadata")) {
            ObjectSchema pageMetadata = new ObjectSchema();
            pageMetadata.setName("PageMetadata");
            pageMetadata.setDescription("Metadata for paginated responses");
            pageMetadata.addProperty("dataType", new Schema<>().type("string").example("page"));
            pageMetadata.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items"));
            pageMetadata.addProperty("count", new Schema<>().type("integer").description("Number of items in current page"));
            pageMetadata.addProperty("pageNumber", new Schema<>().type("integer").description("Current page number"));
            pageMetadata.addProperty("pageSize", new Schema<>().type("integer").description("Items per page"));
            pageMetadata.addProperty("totalPages", new Schema<>().type("integer").description("Total number of pages"));
            pageMetadata.addProperty("isFirst", new Schema<>().type("boolean").description("Whether this is the first page"));
            pageMetadata.addProperty("isLast", new Schema<>().type("boolean").description("Whether this is the last page"));
            pageMetadata.addProperty("hasNext", new Schema<>().type("boolean").description("Whether there is a next page"));
            pageMetadata.addProperty("hasPrevious", new Schema<>().type("boolean").description("Whether there is a previous page"));
            pageMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            schemas.put("PageMetadata", pageMetadata);
        }
        
        // ErrorDetails schema
        if (!schemas.containsKey("ErrorDetails")) {
            ObjectSchema errorDetails = new ObjectSchema();
            errorDetails.setName("ErrorDetails");
            errorDetails.setDescription("Error details for failed responses");
            errorDetails.addProperty("code", new Schema<>().type("string").description("Error code"));
            errorDetails.addProperty("details", new Schema<>().type("string").description("Detailed error message"));
            errorDetails.addProperty("field", new Schema<>().type("string").description("Field that caused the error"));
            schemas.put("ErrorDetails", errorDetails);
        }
        
        // Pageable schema
        if (!schemas.containsKey("Pageable")) {
            ObjectSchema pageable = new ObjectSchema();
            pageable.setName("Pageable");
            pageable.setDescription("Pagination information");
            pageable.addProperty("page", new Schema<>().type("integer").description("Page number"));
            pageable.addProperty("size", new Schema<>().type("integer").description("Page size"));
            pageable.addProperty("sort", new ArraySchema().items(new Schema<>().type("string")).description("Sort criteria"));
            schemas.put("Pageable", pageable);
        }
        
        // Sort schema
        if (!schemas.containsKey("Sort")) {
            ObjectSchema sort = new ObjectSchema();
            sort.setName("Sort");
            sort.setDescription("Sort information");
            sort.addProperty("sorted", new Schema<>().type("boolean").description("Whether sorting is applied"));
            sort.addProperty("unsorted", new Schema<>().type("boolean").description("Whether sorting is not applied"));
            sort.addProperty("empty", new Schema<>().type("boolean").description("Whether sort is empty"));
            schemas.put("Sort", sort);
        }
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
                    // Log rimosso per evitare spam nei log Docker
                } else {
                    // Fallback per tipi primitivi
                    Schema<?> fallbackSchema = createPrimitiveSchema(simpleClassName);
                    if (fallbackSchema != null) {
                        schemas.put(simpleClassName, fallbackSchema);
                        // Log rimosso per evitare spam nei log Docker
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

        // Look for wrapper type data in extensions
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
        
        // Assicurati che il componente sia registrato nell'OpenAPI
        ensureSchemaInComponents(dataClassName, openApi);
        
        switch (wrapperType) {
            case "DTO":
                return createResponseWrapperSchema(dataSchema);
            case "LIST":
                return createResponseWrapperWithListSchema(dataSchema);
            case "PAGE":
                return createResponseWrapperWithPageSchema(dataSchema);
            default:
                return createResponseWrapperSchema(dataSchema);
        }
    }
    
    private void ensureSchemaInComponents(String dataClassName, OpenAPI openApi) {
        String simpleClassName = dataClassName.substring(dataClassName.lastIndexOf('.') + 1);
        
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            @SuppressWarnings("rawtypes")
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            
            if (!schemas.containsKey(simpleClassName)) {
                // Crea uno schema di base se non esiste
                Schema<?> basicSchema = createBasicSchemaForClass(dataClassName);
                if (basicSchema != null) {
                    schemas.put(simpleClassName, basicSchema);
                    // Log rimosso per evitare spam nei log Docker
                }
            }
        }
    }
    
    private Schema<?> createBasicSchemaForClass(String dataClassName) {
        try {
            String simpleClassName = dataClassName.substring(dataClassName.lastIndexOf('.') + 1);
            
            // Per tipi primitivi
            if (dataClassName.equals("java.lang.String")) {
                return new Schema<>().type("string");
            } else if (dataClassName.equals("java.lang.Long")) {
                return new Schema<>().type("integer").format("int64");
            } else if (dataClassName.equals("java.lang.Integer")) {
                return new Schema<>().type("integer").format("int32");
            } else if (dataClassName.equals("java.lang.Boolean")) {
                return new Schema<>().type("boolean");
            }
            
            // Per altri tipi, crea un schema object generico
            ObjectSchema schema = new ObjectSchema();
            schema.setTitle(simpleClassName);
            schema.setDescription("Auto-generated schema for " + simpleClassName);
            return schema;
            
        } catch (Exception e) {
            System.err.println("Error creating basic schema for " + dataClassName + ": " + e.getMessage());
            return null;
        }
    }

    private Schema<?> createDataSchema(String className) {
        if (className == null || className.isEmpty()) {
            return new Schema<>().type("object");
        }
        
        // Gestisci i tipi primitivi
        if (className.equals("java.lang.String")) {
            return new Schema<>().type("string");
        } else if (className.equals("java.lang.Long")) {
            return new Schema<>().type("integer").format("int64");
        } else if (className.equals("java.lang.Integer")) {
            return new Schema<>().type("integer").format("int32");
        } else if (className.equals("java.lang.Boolean")) {
            return new Schema<>().type("boolean");
        } else if (className.equals("java.lang.Double")) {
            return new Schema<>().type("number").format("double");
        } else if (className.equals("java.lang.Float")) {
            return new Schema<>().type("number").format("float");
        } else if (className.equals("java.math.BigDecimal")) {
            return new Schema<>().type("number");
        } else if (className.equals("java.time.LocalDateTime")) {
            return new Schema<>().type("string").format("date-time");
        } else if (className.equals("java.time.LocalDate")) {
            return new Schema<>().type("string").format("date");
        }
        
        // Per i tipi custom, crea un riferimento
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
        wrapper.addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Response timestamp"));
        wrapper.addProperty("error", new ObjectSchema().$ref("#/components/schemas/ErrorDetails"));
        
        // Single metadata
        Schema<?> metadataSchema = new Schema<>();
        metadataSchema.set$ref("#/components/schemas/SingleMetadata");
        wrapper.addProperty("metadata", metadataSchema);
        
        return wrapper;
    }

    private Schema<?> createResponseWrapperWithListSchema(Schema<?> dataSchema) {
        ObjectSchema wrapper = new ObjectSchema();
        wrapper.addProperty("success", new Schema<>().type("boolean").description("Indicates if the operation was successful"));
        wrapper.addProperty("message", new Schema<>().type("string").description("Response message"));
        wrapper.addProperty("data", new ArraySchema().items(dataSchema));
        wrapper.addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Response timestamp"));
        wrapper.addProperty("error", new ObjectSchema().$ref("#/components/schemas/ErrorDetails"));
        
        // List metadata
        Schema<?> metadataSchema = new Schema<>();
        metadataSchema.set$ref("#/components/schemas/ListMetadata");
        wrapper.addProperty("metadata", metadataSchema);
        
        return wrapper;
    }

    private Schema<?> createResponseWrapperWithPageSchema(Schema<?> dataSchema) {
        ObjectSchema wrapper = new ObjectSchema();
        wrapper.addProperty("success", new Schema<>().type("boolean").description("Indicates if the operation was successful"));
        wrapper.addProperty("message", new Schema<>().type("string").description("Response message"));
        
        // For PAGE, the data is a Page<T> object, not just an array
        ObjectSchema pageSchema = new ObjectSchema();
        pageSchema.addProperty("content", new ArraySchema().items(dataSchema));
        pageSchema.addProperty("pageable", new ObjectSchema().$ref("#/components/schemas/Pageable"));
        pageSchema.addProperty("totalPages", new Schema<>().type("integer"));
        pageSchema.addProperty("totalElements", new Schema<>().type("integer").format("int64"));
        pageSchema.addProperty("last", new Schema<>().type("boolean"));
        pageSchema.addProperty("first", new Schema<>().type("boolean"));
        pageSchema.addProperty("numberOfElements", new Schema<>().type("integer"));
        pageSchema.addProperty("size", new Schema<>().type("integer"));
        pageSchema.addProperty("number", new Schema<>().type("integer"));
        pageSchema.addProperty("sort", new ObjectSchema().$ref("#/components/schemas/Sort"));
        pageSchema.addProperty("empty", new Schema<>().type("boolean"));
        
        wrapper.addProperty("data", pageSchema);
        wrapper.addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Response timestamp"));
        wrapper.addProperty("error", new ObjectSchema().$ref("#/components/schemas/ErrorDetails"));
        
        // Page metadata
        Schema<?> metadataSchema = new Schema<>();
        metadataSchema.set$ref("#/components/schemas/PageMetadata");
        wrapper.addProperty("metadata", metadataSchema);
        
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
        
        // Sostituisce completamente la risposta esistente per questo response code
        operation.getResponses().addApiResponse(responseCode, response);
        
        // Rimuove eventuali risposte generiche che potrebbero confondere i generatori
        if ("200".equals(responseCode)) {
            // Mantieni solo le risposte di errore standard
            operation.getResponses().entrySet().removeIf(entry -> 
                !entry.getKey().equals("200") && 
                !entry.getKey().equals("400") && 
                !entry.getKey().equals("401") && 
                !entry.getKey().equals("403") && 
                !entry.getKey().equals("404") && 
                !entry.getKey().equals("500")
            );
        }
    }
}
