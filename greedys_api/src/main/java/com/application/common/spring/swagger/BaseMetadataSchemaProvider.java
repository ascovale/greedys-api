package com.application.common.spring.swagger;

import java.util.Map;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class per la configurazione dei metadati di base OpenAPI
 * Questa classe si occupa dell'inizializzazione della struttura base OpenAPI
 * e della configurazione dei componenti necessari per il processo di customizzazione.
 */
@Slf4j
public class BaseMetadataSchemaProvider {

    /**
     * Configura i metadati di base per OpenAPI
     * Inizializza la struttura components/schemas se non esiste
     * 
     * @param openApi Specifica OpenAPI da configurare
     */
    public void setupBaseMetadata(OpenAPI openApi) {
        log.debug("BaseMetadataSchemaProvider: Configurazione metadati di base OpenAPI");
        
        // Assicura che il componente Components esista
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
            log.debug("Creato oggetto Components mancante");
        }
        
        // Assicura che la mappa schemas esista
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new java.util.HashMap<>());
            log.debug("Creata mappa schemas mancante");
        }
        
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        int initialSchemaCount = schemas.size();
        
        log.info("BaseMetadataSchemaProvider: Configurazione completata. Schemi esistenti: {}", initialSchemaCount);
    }

    /**
     * Aggiunge schemi di base comuni se necessari
     * Include ErrorDetails, LocalDateTime e BaseMetadata che sono sempre referenziati
     * 
     * @param openApi Specifica OpenAPI
     */
    public void addCommonSchemas(OpenAPI openApi) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        // ✅ NEW: Genera schema per ResponseWrapperError se non esiste
        if (!schemas.containsKey("ResponseWrapperError")) {
            generateResponseWrapperErrorSchema(schemas);
        }
        
        // ✅ NEW: Genera schemi per tipi primitivi se non esistono
        generatePrimitiveTypeSchemas(schemas);
        
        // Genera schema per ErrorDetails se non esiste
        if (!schemas.containsKey("ErrorDetails")) {
            generateErrorDetailsSchema(schemas);
        }
        
        // Genera schema per LocalDateTime se non esiste  
        if (!schemas.containsKey("LocalDateTime")) {
            generateLocalDateTimeSchema(schemas);
        }
        
        // Genera schema per BaseMetadata se non esiste
        if (!schemas.containsKey("BaseMetadata")) {
            generateBaseMetadataSchema(schemas);
        }
        
        // Genera schemi per metadata specifici
        if (!schemas.containsKey("SingleMetadata")) {
            generateSingleMetadataSchema(schemas);
        }
        
        if (!schemas.containsKey("ListMetadata")) {
            generateListMetadataSchema(schemas);
        }
        
        if (!schemas.containsKey("PageMetadata")) {
            generatePageMetadataSchema(schemas);
        }
        
        log.debug("BaseMetadataSchemaProvider: Schemi comuni verificati/aggiunti");
    }
    
    /**
     * Genera schema per ErrorDetails usando SpringDoc reflection
     */
    private void generateErrorDetailsSchema(@SuppressWarnings("rawtypes") Map<String, Schema> schemas) {
        try {
            Class<?> errorDetailsClass = Class.forName("com.application.common.web.ErrorDetails");
            io.swagger.v3.core.converter.AnnotatedType annotatedType = 
                new io.swagger.v3.core.converter.AnnotatedType(errorDetailsClass);
            
            io.swagger.v3.core.converter.ResolvedSchema resolvedSchema = 
                io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .readAllAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.schema != null) {
                schemas.put("ErrorDetails", resolvedSchema.schema);
                log.debug("Schema ErrorDetails generato con SpringDoc");
                
                // Aggiungi anche schemi referenziati se presenti
                if (resolvedSchema.referencedSchemas != null) {
                    schemas.putAll(resolvedSchema.referencedSchemas);
                }
            }
        } catch (Exception e) {
            log.warn("Errore generazione schema ErrorDetails: {}", e.getMessage());
        }
    }
    
    /**
     * Genera schema per LocalDateTime
     */
    private void generateLocalDateTimeSchema(@SuppressWarnings("rawtypes") Map<String, Schema> schemas) {
        @SuppressWarnings("rawtypes")
        Schema localDateTimeSchema = new Schema();
        localDateTimeSchema.setType("string");
        localDateTimeSchema.setFormat("date-time");
        localDateTimeSchema.setDescription("ISO 8601 date-time format");
        localDateTimeSchema.setExample("2023-12-25T10:30:00");
        
        schemas.put("LocalDateTime", localDateTimeSchema);
        log.debug("Schema LocalDateTime generato");
    }
    
    /**
     * Genera schema per BaseMetadata usando SpringDoc reflection
     */
    private void generateBaseMetadataSchema(@SuppressWarnings("rawtypes") Map<String, Schema> schemas) {
        try {
            Class<?> baseMetadataClass = Class.forName("com.application.common.web.metadata.BaseMetadata");
            io.swagger.v3.core.converter.AnnotatedType annotatedType = 
                new io.swagger.v3.core.converter.AnnotatedType(baseMetadataClass);
            
            io.swagger.v3.core.converter.ResolvedSchema resolvedSchema = 
                io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .readAllAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.schema != null) {
                schemas.put("BaseMetadata", resolvedSchema.schema);
                log.debug("Schema BaseMetadata generato con SpringDoc");
                
                // Aggiungi anche schemi referenziati se presenti
                if (resolvedSchema.referencedSchemas != null) {
                    schemas.putAll(resolvedSchema.referencedSchemas);
                }
            }
        } catch (Exception e) {
            log.warn("Errore generazione schema BaseMetadata: {}", e.getMessage());
        }
    }

    /**
     * Valida la configurazione OpenAPI prima del processo di customizzazione
     * 
     * @param openApi Specifica OpenAPI da validare
     * @return true se la configurazione è valida, false altrimenti
     */
    public boolean validateOpenApiStructure(OpenAPI openApi) {
        if (openApi == null) {
            log.error("BaseMetadataSchemaProvider: OpenAPI è null");
            return false;
        }
        
        if (openApi.getComponents() == null) {
            log.error("BaseMetadataSchemaProvider: Components è null");
            return false;
        }
        
        if (openApi.getComponents().getSchemas() == null) {
            log.error("BaseMetadataSchemaProvider: Schemas è null");
            return false;
        }
        
        if (openApi.getPaths() == null) {
            log.warn("BaseMetadataSchemaProvider: Paths è null - nessuna API da processare");
            return true; // Non è un errore fatale
        }
        
        log.debug("BaseMetadataSchemaProvider: Struttura OpenAPI validata con successo");
        return true;
    }

    /**
     * Fornisce statistiche sulla struttura OpenAPI per debugging
     * 
     * @param openApi Specifica OpenAPI da analizzare
     * @return Stringa con statistiche leggibili
     */
    public String getOpenApiStats(OpenAPI openApi) {
        if (openApi == null) {
            return "OpenAPI: null";
        }
        
        int pathCount = (openApi.getPaths() != null) ? openApi.getPaths().size() : 0;
        int schemaCount = 0;
        
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            schemaCount = openApi.getComponents().getSchemas().size();
        }
        
        return String.format("OpenAPI stats: %d paths, %d existing schemas", pathCount, schemaCount);
    }
    
    /**
     * Genera schema per SingleMetadata usando SpringDoc reflection
     */
    private void generateSingleMetadataSchema(@SuppressWarnings("rawtypes") Map<String, Schema> schemas) {
        try {
            Class<?> singleMetadataClass = Class.forName("com.application.common.web.metadata.SingleMetadata");
            io.swagger.v3.core.converter.AnnotatedType annotatedType = 
                new io.swagger.v3.core.converter.AnnotatedType(singleMetadataClass);
            
            io.swagger.v3.core.converter.ResolvedSchema resolvedSchema = 
                io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .readAllAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.schema != null) {
                schemas.put("SingleMetadata", resolvedSchema.schema);
                log.debug("Schema SingleMetadata generato con SpringDoc");
                
                if (resolvedSchema.referencedSchemas != null) {
                    schemas.putAll(resolvedSchema.referencedSchemas);
                }
            }
        } catch (Exception e) {
            log.warn("Errore generazione schema SingleMetadata: {}", e.getMessage());
        }
    }
    
    /**
     * Genera schema per ListMetadata usando SpringDoc reflection
     */
    private void generateListMetadataSchema(@SuppressWarnings("rawtypes") Map<String, Schema> schemas) {
        try {
            Class<?> listMetadataClass = Class.forName("com.application.common.web.metadata.ListMetadata");
            io.swagger.v3.core.converter.AnnotatedType annotatedType = 
                new io.swagger.v3.core.converter.AnnotatedType(listMetadataClass);
            
            io.swagger.v3.core.converter.ResolvedSchema resolvedSchema = 
                io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .readAllAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.schema != null) {
                schemas.put("ListMetadata", resolvedSchema.schema);
                log.debug("Schema ListMetadata generato con SpringDoc");
                
                if (resolvedSchema.referencedSchemas != null) {
                    schemas.putAll(resolvedSchema.referencedSchemas);
                }
            }
        } catch (Exception e) {
            log.warn("Errore generazione schema ListMetadata: {}", e.getMessage());
        }
    }
    
    /**
     * Genera schema per PageMetadata usando SpringDoc reflection
     */
    private void generatePageMetadataSchema(@SuppressWarnings("rawtypes") Map<String, Schema> schemas) {
        try {
            Class<?> pageMetadataClass = Class.forName("com.application.common.web.metadata.PageMetadata");
            io.swagger.v3.core.converter.AnnotatedType annotatedType = 
                new io.swagger.v3.core.converter.AnnotatedType(pageMetadataClass);
            
            io.swagger.v3.core.converter.ResolvedSchema resolvedSchema = 
                io.swagger.v3.core.converter.ModelConverters.getInstance()
                    .readAllAsResolvedSchema(annotatedType);
            
            if (resolvedSchema != null && resolvedSchema.schema != null) {
                schemas.put("PageMetadata", resolvedSchema.schema);
                log.debug("Schema PageMetadata generato con SpringDoc");
                
                if (resolvedSchema.referencedSchemas != null) {
                    schemas.putAll(resolvedSchema.referencedSchemas);
                }
            }
        } catch (Exception e) {
            log.warn("Errore generazione schema PageMetadata: {}", e.getMessage());
        }
    }
    
    /**
     * ✅ NEW: Genera schema per ResponseWrapperError
     * Schema dedicato per le error responses (400, 401, 403, 404, 409, 500)
     */
    @SuppressWarnings("rawtypes")
    private void generateResponseWrapperErrorSchema(Map<String, Schema> schemas) {
        io.swagger.v3.oas.models.media.ObjectSchema errorWrapperSchema = new io.swagger.v3.oas.models.media.ObjectSchema();
        errorWrapperSchema
            .title("API Error Response")
            .description("Standard API response wrapper for HTTP 4xx and 5xx error cases")
            .addProperty("success", new io.swagger.v3.oas.models.media.BooleanSchema()
                .example(false)
                .description("Always false for error responses"))
            .addProperty("data", new io.swagger.v3.oas.models.media.Schema<>()
                .nullable(true)
                .description("Always null for error responses")
                .example(null))
            .addProperty("message", new io.swagger.v3.oas.models.media.StringSchema()
                .description("Human-readable error message")
                .example("Customer not found"))
            .addProperty("timestamp", new io.swagger.v3.oas.models.media.StringSchema()
                .format("date-time")
                .description("Error occurrence timestamp")
                .example("2023-12-25T10:30:00Z"))
            .addProperty("error", new io.swagger.v3.oas.models.media.Schema<>()
                .$ref("#/components/schemas/ErrorDetails")
                .description("Structured error information"))
            .addProperty("metadata", new io.swagger.v3.oas.models.media.Schema<>()
                .$ref("#/components/schemas/SingleMetadata")
                .description("Additional error context"));
        
        errorWrapperSchema.required(java.util.Arrays.asList("success", "timestamp"));
        
        schemas.put("ResponseWrapperError", errorWrapperSchema);
        log.info("✅ GENERATOR: Schema ResponseWrapperError creato per error responses");
    }
    
    /**
     * ✅ NEW: Genera schemi per tipi primitivi mancanti
     * Risolve i problemi di generazione per Boolean, String, LocalDate, Long
     */
    @SuppressWarnings("rawtypes")
    private void generatePrimitiveTypeSchemas(Map<String, Schema> schemas) {
        // Boolean schema
        if (!schemas.containsKey("Boolean")) {
            io.swagger.v3.oas.models.media.BooleanSchema booleanSchema = new io.swagger.v3.oas.models.media.BooleanSchema();
            booleanSchema.description("Boolean value").example(true);
            schemas.put("Boolean", booleanSchema);
        }
        
        // String schema  
        if (!schemas.containsKey("String")) {
            io.swagger.v3.oas.models.media.StringSchema stringSchema = new io.swagger.v3.oas.models.media.StringSchema();
            stringSchema.description("String value").example("Sample text");
            schemas.put("String", stringSchema);
        }
        
        // LocalDate schema
        if (!schemas.containsKey("LocalDate")) {
            io.swagger.v3.oas.models.media.StringSchema localDateSchema = new io.swagger.v3.oas.models.media.StringSchema();
            localDateSchema.format("date").description("ISO 8601 date format").example("2023-12-25");
            schemas.put("LocalDate", localDateSchema);
        }
        
        // Long schema
        if (!schemas.containsKey("Long")) {
            io.swagger.v3.oas.models.media.IntegerSchema longSchema = new io.swagger.v3.oas.models.media.IntegerSchema();
            longSchema.format("int64").description("Long integer value").example(123456789L);
            schemas.put("Long", longSchema);
        }
        
        // Integer schema
        if (!schemas.containsKey("Integer")) {
            io.swagger.v3.oas.models.media.IntegerSchema integerSchema = new io.swagger.v3.oas.models.media.IntegerSchema();
            integerSchema.format("int32").description("Integer value").example(12345);
            schemas.put("Integer", integerSchema);
        }
        
        log.debug("✅ GENERATOR: Schemi primitivi verificati/aggiunti (Boolean, String, LocalDate, Long, Integer)");
    }
}
