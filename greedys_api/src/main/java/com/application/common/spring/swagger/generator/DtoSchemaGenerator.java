package com.application.common.spring.swagger.generator;

import java.util.Map;

import com.application.common.spring.swagger.generator.helper.SchemaHelper;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Generatore per schemi DTO usando SpringDoc ModelConverters
 */
@UtilityClass
@Slf4j
public class DtoSchemaGenerator {
    
    public static void generateDataSchema(String dataClassName, OpenAPI openApi) {
        if (dataClassName == null) return;
        
        String schemaName = SchemaHelper.extractSimpleClassName(dataClassName);
        
        // Se già esiste, non rigenerare
        if (openApi.getComponents().getSchemas().containsKey(schemaName)) {
            return;
        }
        
        Schema<?> dataSchema = createSchemaUsingSpringDoc(dataClassName);
        
        if (dataSchema != null) {
            openApi.getComponents().getSchemas().put(schemaName, dataSchema);
            log.debug("Generated DTO schema via SpringDoc: {}", schemaName);
            addReferencedSchemas(dataClassName, openApi.getComponents().getSchemas());
        } else {
            generateFallbackSchema(schemaName, openApi);
        }
    }
    
    private static Schema<?> createSchemaUsingSpringDoc(String fullClassName) {
        try {
            Class<?> clazz = Class.forName(fullClassName);
            AnnotatedType annotatedType = new AnnotatedType(clazz);
            ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(annotatedType);
            return resolvedSchema.schema;
        } catch (Exception e) {
            log.warn("Failed to create schema for {}: {}", fullClassName, e.getMessage());
            return null;
        }
    }
    
    @SuppressWarnings("rawtypes")
    private static void addReferencedSchemas(String dataClassName, Map<String, Schema> schemas) {
        try {
            Class<?> clazz = Class.forName(dataClassName);
            AnnotatedType annotatedType = new AnnotatedType(clazz);
            ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(annotatedType);
            
            if (resolvedSchema.referencedSchemas != null) {
                resolvedSchema.referencedSchemas.forEach((name, schema) -> {
                    if (!schemas.containsKey(name)) {
                        schemas.put(name, schema);
                        log.debug("Added referenced schema: {}", name);
                    }
                });
            }
        } catch (Exception e) {
            log.debug("Could not add referenced schemas for {}: {}", dataClassName, e.getMessage());
        }
    }
    
    private static void generateFallbackSchema(String schemaName, OpenAPI openApi) {
        Schema<?> fallbackSchema = new ObjectSchema()
            .title("Data model for " + schemaName)
            .description("Fallback schema for " + schemaName);
        openApi.getComponents().getSchemas().put(schemaName, fallbackSchema);
        log.warn("Used fallback schema for: {}", schemaName);
    }
}
