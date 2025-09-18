package com.application.common.spring.swagger.customizer;

import java.util.Collection;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.application.common.spring.swagger.generator.DefaultSchemaGenerator;
import com.application.common.spring.swagger.generator.DtoSchemaGenerator;
import com.application.common.spring.swagger.generator.OperationResponseUpdater;
import com.application.common.spring.swagger.generator.WrapperSchemaGenerator;
import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.metadata.WrapperCategory;
import com.application.common.spring.swagger.registry.MetadataRegistry;
import com.application.common.spring.swagger.util.VendorExtensionsHelper;
// ...existing imports...
import com.application.common.spring.swagger.utility.SchemaTypeAnalyzer;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generatore coordinatore per schemi e aggiornamento operazioni OpenAPI.
 * 
 * Usa la nuova architettura modulare con generatori specializzati:
 * - DefaultSchemaGenerator: schemi base (metadata, errori)
 * - DtoSchemaGenerator: schemi DTO usando ModelConverters
 * - WrapperSchemaGenerator: schemi wrapper (ResponseWrapperXXX) 
 * - OperationResponseUpdater: aggiornamento responses delle operazioni
 */
@Component
@Order(1000)  // Dopo MetadataCollector (500)
@RequiredArgsConstructor
@Slf4j
public class SwaggerSpecificationGenerator implements OpenApiCustomizer {
    
    private final MetadataRegistry registry;
    
    @Override
    public void customise(OpenAPI openApi) {
        try {
            // 1. Ottieni tutti i metadati delle operazioni dal registry
            Collection<OperationDataMetadata> allOperations = registry.getAllCompleteData();
            
            if (allOperations.isEmpty()) {
                log.warn("No operation metadata found in registry - schema generation skipped");
                return;
            }
            
            log.info("Generating schemas for {} operations", allOperations.size());
            
            // 2. Analizza i tipi di schemi da generare
            SchemaTypeAnalyzer.SchemaAnalysisResult schemaAnalysis = 
                SchemaTypeAnalyzer.analyzeSchemaTypes(allOperations);
            
            log.info("Schema analysis: {}", schemaAnalysis.getSummary());
            
            // 3. Genera schemi comuni (errori, metadata)
            DefaultSchemaGenerator.generateCommonSchemas(openApi);
            
            // 4. Genera schemi DTO dai ModelConverters
            for (String dtoClassName : schemaAnalysis.getDtoSchemasToExtract()) {
                if (dtoClassName != null && !dtoClassName.trim().isEmpty()) {
                    DtoSchemaGenerator.generateDataSchema(dtoClassName, openApi);
                } else {
                    log.warn("Skipping null or empty DTO class name in schema generation");
                }
            }
            
            // 5. Genera schemi wrapper (ResponseWrapperXXX) - con informazioni sui tipi e categorie
            Map<String, String> wrapperToClassMapping = schemaAnalysis.getWrapperToDataClassMapping();
            Map<String, WrapperCategory> wrapperToCategory = schemaAnalysis.getWrapperToCategoryMapping();
            for (String wrapperSchemaName : schemaAnalysis.getWrapperSchemasToGenerate()) {
                String dataClassName = wrapperToClassMapping.get(wrapperSchemaName);
                // Prende la categoria dalla mappa calcolata; fallback a deduzione dal nome se assente
                WrapperCategory category = wrapperToCategory.get(wrapperSchemaName);
                WrapperSchemaGenerator.generateWrapperSchema(wrapperSchemaName, dataClassName, category, openApi);
            }

            // 7. Genera catalogo JSON dei ResponseWrapper prodotti (wrapper -> T, categoria)
            com.application.common.spring.swagger.catalog.ResponseWrapperCatalogRegistry.generateAndSave(
                wrapperToClassMapping, wrapperToCategory, schemaAnalysis.getWrapperSchemasToGenerate()
            );
            
            // 6. Aggiorna le operazioni con i nuovi schemi
            updateOperations(allOperations, openApi);
            
            log.info("Schema generation completed successfully - {} total schemas", 
                openApi.getComponents().getSchemas().size());
            
        } catch (Exception e) {
            log.error("Error during OpenAPI customization: {}", e.getMessage(), e);
        }
    }
    
  
    
    /**
     * Aggiorna le operazioni con i riferimenti agli schemi generati
     */
    private void updateOperations(Collection<OperationDataMetadata> allOperations, OpenAPI openApi) {
        allOperations.forEach(operationMetadata -> {
            String operationId = operationMetadata.getOperationId();
            
            // Trova l'operazione corrispondente nell'OpenAPI spec
            openApi.getPaths().forEach((pathKey, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    if (operationId.equals(operation.getOperationId())) {
                        OperationResponseUpdater.updateOperationResponses(operation, operationMetadata);
                        
                        // Aggiungi vendor extensions centralizzate per l'operazione
                        VendorExtensionsHelper.addOperationVendorExtensions(operation, operationMetadata);
                        
                        log.debug("Updated operation responses: {}", operationId);
                    }
                });
            });
        });
    }
  
}
