package com.application.common.spring.swagger.customizer;

import java.util.Collection;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.application.common.spring.swagger.generator.DefaultSchemaGenerator;
import com.application.common.spring.swagger.generator.DtoSchemaGenerator;
import com.application.common.spring.swagger.generator.OperationResponseUpdater;
import com.application.common.spring.swagger.generator.WrapperSchemaGenerator;
import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.registry.MetadataRegistry;
import com.application.common.spring.swagger.utility.SchemaTypeAnalyzer;
import com.application.common.spring.swagger.vendor.VendorExtensionApplicator;

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
    private final VendorExtensionApplicator vendorExtensionApplicator;
    
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
            
            // 5. Genera schemi wrapper (ResponseWrapperXXX) - versione semplificata
            for (String wrapperSchemaName : schemaAnalysis.getWrapperSchemasToGenerate()) {
                WrapperSchemaGenerator.generateWrapperSchema(wrapperSchemaName, openApi);
            }
            
            // 6. Aggiorna le operazioni con i nuovi schemi
            updateOperations(allOperations, openApi);
            
            // 7. Applica vendor extensions a tutti i livelli
            applyVendorExtensions(openApi);
            
            log.info("Schema generation completed successfully - {} total schemas", 
                openApi.getComponents().getSchemas().size());
            
        } catch (Exception e) {
            log.error("Error during OpenAPI customization: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Applica vendor extensions a tutti i livelli dell'OpenAPI spec
     */
    private void applyVendorExtensions(OpenAPI openApi) {
        try {
            log.debug("Applying vendor extensions...");
            
            // 1. Extensions globali
            vendorExtensionApplicator.applyGlobalExtensions(openApi);
            
            // 2. Extensions alle operazioni
            vendorExtensionApplicator.applyOperationExtensions(openApi);
            
            // 3. Extensions agli schemi wrapper
            vendorExtensionApplicator.applyWrapperSchemaExtensions(openApi);
            
            log.info("Vendor extensions applied successfully");
            
        } catch (Exception e) {
            log.error("Error applying vendor extensions: {}", e.getMessage(), e);
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
                        log.debug("Updated operation responses: {}", operationId);
                    }
                });
            });
        });
    }
}
