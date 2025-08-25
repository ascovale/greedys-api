package com.application.common.spring.swagger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
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
 * appropriate schemas for ResponseWrapper with different metadata types.
 * 
 * SOLUZIONE CORRETTA: Usa SpringDoc ModelConverters per generare schemi completi
 * invece di ricostruzione manuale con reflection.
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
        
        // Crea gli schemi mancanti usando SpringDoc invece di reflection manuale
        createSchemasUsingSpringDoc(referencedClasses, openApi);

        // Process each path and operation
        openApi.getPaths().forEach((path, pathItem) -> {
            processPathItem(pathItem, openApi);
        });
    }
    
    private void addRequiredSchemas(OpenAPI openApi) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        // Metadata schemas are handled by MetadataSchemaCustomizer
        // Only handle ErrorDetails, Pageable, and Sort here
        
        // ErrorDetails schema - lasciare che SpringDoc lo generi automaticamente dalla classe Java annotata
        // Rimuoviamo la creazione manuale per evitare conflitti
        
        // Forza la creazione dello schema ErrorDetails dalla classe Java
        if (!schemas.containsKey("ErrorDetails")) {
            try {
                Class<?> errorDetailsClass = Class.forName("com.application.common.web.ErrorDetails");
                Schema<?> errorDetailsSchema = createSchemaUsingSpringDoc(errorDetailsClass.getName());
                if (errorDetailsSchema != null) {
                    schemas.put("ErrorDetails", errorDetailsSchema);
                }
            } catch (ClassNotFoundException e) {
                System.err.println("ErrorDetails class not found: " + e.getMessage());
            }
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

    /**
     * SOLUZIONE CORRETTA: Usa SpringDoc ModelConverters per creare schemi completi
     * invece di ricostruzione manuale con reflection
     */
    private void createSchemasUsingSpringDoc(Set<String> referencedClasses, OpenAPI openApi) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        for (String fullClassName : referencedClasses) {
            String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
            
            if (!schemas.containsKey(simpleClassName)) {
                // SOLUZIONE CORRETTA: Usa SpringDoc per creare lo schema completo
                Schema<?> springDocSchema = createSchemaUsingSpringDoc(fullClassName);
                
                if (springDocSchema != null) {
                    schemas.put(simpleClassName, springDocSchema);
                    
                    // SpringDoc restituisce anche tutti gli schemi referenziati
                    // quindi processali tutti in una volta
                    Map<String, Schema<?>> additionalSchemas = getAdditionalSchemasFromSpringDoc(fullClassName);
                    if (additionalSchemas != null) {
                        for (Map.Entry<String, Schema<?>> entry : additionalSchemas.entrySet()) {
                            if (!schemas.containsKey(entry.getKey())) {
                                schemas.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    
                } else {
                    // FALLBACK: Non creare componenti schema per i primitivi Java
                    if (isPrimitiveJavaType(fullClassName)) {
                        // I tipi primitivi Java sono gestiti automaticamente da OpenAPI
                        // come tipi inline, non devono essere componenti schema
                        continue; 
                    }
                    
                    // Per altri tipi non primitivi, crea schema generico
                    Schema<?> fallbackSchema = new Schema<>();
                    fallbackSchema.setType("object");
                    fallbackSchema.setDescription("Auto-generated schema for " + simpleClassName);
                    schemas.put(simpleClassName, fallbackSchema);
                }
            }
        }
    }
    
    /**
     * SOLUZIONE CORRETTA: Usa SpringDoc per creare schemi completi
     * invece di ricostruzione manuale con reflection
     */
    private Schema<?> createSchemaUsingSpringDoc(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            
            // Usa SpringDoc ModelConverters per ottenere lo schema corretto con tutti i tipi
            AnnotatedType annotatedType = new AnnotatedType(clazz);
            ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.schema != null) {
                return resolvedSchema.schema;
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found for SpringDoc schema generation: " + fullClassName);
        } catch (Exception e) {
            System.err.println("Error creating SpringDoc schema for " + fullClassName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Ottiene tutti gli schemi aggiuntivi generati da SpringDoc
     * (tipi referenziati, nested objects, ecc.)
     */
    private Map<String, Schema<?>> getAdditionalSchemasFromSpringDoc(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            
            // Usa SpringDoc ModelConverters per ottenere TUTTI gli schemi correlati
            AnnotatedType annotatedType = new AnnotatedType(clazz);
            ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.referencedSchemas != null) {
                // Crea una mappa compatibile
                Map<String, Schema<?>> result = new HashMap<>();
                for (Map.Entry<String, ?> entry : resolvedSchema.referencedSchemas.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Schema<Object> schema = (Schema<Object>) entry.getValue();
                    result.put(entry.getKey(), schema);
                }
                return result;
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found for SpringDoc additional schemas: " + fullClassName);
        } catch (Exception e) {
            System.err.println("Error getting additional SpringDoc schemas for " + fullClassName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    private boolean isPrimitiveJavaType(String fullClassName) {
        // Tipi primitivi Java che non devono diventare componenti schema
        // OpenAPI li gestisce automaticamente come tipi inline
        return fullClassName.equals("java.lang.String") ||
               fullClassName.equals("java.lang.Long") ||
               fullClassName.equals("java.lang.Integer") ||
               fullClassName.equals("java.lang.Boolean") ||
               fullClassName.equals("java.lang.Double") ||
               fullClassName.equals("java.lang.Float") ||
               fullClassName.equals("java.math.BigDecimal") ||
               fullClassName.equals("java.time.LocalDate") ||
               fullClassName.equals("java.time.LocalDateTime") ||
               fullClassName.equals("java.time.Instant") ||
               fullClassName.equals("java.util.Date") ||
               // Tipi primitivi base
               fullClassName.equals("long") ||
               fullClassName.equals("int") ||
               fullClassName.equals("boolean") ||
               fullClassName.equals("double") ||
               fullClassName.equals("float") ||
               // Pattern generici per pacchetti Java standard che sono sempre primitivi
               (fullClassName.startsWith("java.lang.") && isPrimitiveWrapper(fullClassName)) ||
               (fullClassName.startsWith("java.time.") && !fullClassName.contains("$"));
    }
    
    private boolean isPrimitiveWrapper(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return simpleName.equals("String") ||
               simpleName.equals("Integer") ||
               simpleName.equals("Long") ||
               simpleName.equals("Double") ||
               simpleName.equals("Float") ||
               simpleName.equals("Boolean") ||
               simpleName.equals("Byte") ||
               simpleName.equals("Short") ||
               simpleName.equals("Character");
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
        
        // ✅ FIX: RESTITUISCI DIRETTAMENTE LO SCHEMA WRAPPER (non reference)
        // Questo forza l'uso dei nostri wrapper con le specifiche di tipo corrette
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
                // Usa SpringDoc per creare lo schema se non esiste
                Schema<?> springDocSchema = createSchemaUsingSpringDoc(dataClassName);
                if (springDocSchema != null) {
                    schemas.put(simpleClassName, springDocSchema);
                }
            }
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
        
        // Per ErrorDetails: crea solo il riferimento senza altri attributi
        Schema<?> errorSchema = new Schema<>();
        errorSchema.set$ref("#/components/schemas/ErrorDetails");
        wrapper.addProperty("error", errorSchema);
        
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
        
        // Per ErrorDetails: crea solo il riferimento senza altri attributi
        Schema<?> errorSchema = new Schema<>();
        errorSchema.set$ref("#/components/schemas/ErrorDetails");
        wrapper.addProperty("error", errorSchema);
        
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
        
        // Crea riferimenti puliti per Pageable e Sort
        Schema<?> pageableSchema = new Schema<>();
        pageableSchema.set$ref("#/components/schemas/Pageable");
        pageSchema.addProperty("pageable", pageableSchema);
        
        pageSchema.addProperty("totalPages", new Schema<>().type("integer"));
        pageSchema.addProperty("totalElements", new Schema<>().type("integer").format("int64"));
        pageSchema.addProperty("last", new Schema<>().type("boolean"));
        pageSchema.addProperty("first", new Schema<>().type("boolean"));
        pageSchema.addProperty("numberOfElements", new Schema<>().type("integer"));
        pageSchema.addProperty("size", new Schema<>().type("integer"));
        pageSchema.addProperty("number", new Schema<>().type("integer"));
        
        Schema<?> sortSchema = new Schema<>();
        sortSchema.set$ref("#/components/schemas/Sort");
        pageSchema.addProperty("sort", sortSchema);
        
        pageSchema.addProperty("empty", new Schema<>().type("boolean"));
        
        wrapper.addProperty("data", pageSchema);
        wrapper.addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Response timestamp"));
        
        // Per ErrorDetails: crea solo il riferimento senza altri attributi
        Schema<?> errorSchema = new Schema<>();
        errorSchema.set$ref("#/components/schemas/ErrorDetails");
        wrapper.addProperty("error", errorSchema);
        
        // Page metadata
        Schema<?> metadataSchema = new Schema<>();
        metadataSchema.set$ref("#/components/schemas/PageMetadata");
        wrapper.addProperty("metadata", metadataSchema);
        
        return wrapper;
    }

    private void updateOperationResponse(Operation operation, Schema<?> schema, String description, String responseCode) {
        // ✅ FIX: Usa SEMPRE lo schema passato (che è già il wrapper corretto dai metodi create*)
        // Non creare schemi specifici qui - usa quello che abbiamo già costruito correttamente
        ApiResponse response = new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", 
                                new MediaType().schema(schema)));
        
        if (operation.getResponses() == null) {
            operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses());
        }
        
        // SOLUZIONE AI DOPPIONI: Sostituisce solo il response specifico
        // senza interferire con gli altri response codes (200, 201, etc.)
        operation.getResponses().addApiResponse(responseCode, response);
    }
}