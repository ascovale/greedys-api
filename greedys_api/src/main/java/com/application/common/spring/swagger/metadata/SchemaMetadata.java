package com.application.common.spring.swagger.metadata;

import lombok.Builder;
import lombok.Data;

/**
 * Metadati per la generazione degli schemi Swagger.
 * 
 * Contiene solo informazioni relative alla generazione di schemi,
 * separate dai metadati delle operazioni HTTP.
 */
@Data
@Builder
public class SchemaMetadata {
    
    /**
     * Chiave univoca per questo metadata (solitamente operationId)
     */
    private final String key;
    
    /**
     * Nome completo della classe dati (es: "com.example.dto.RestaurantDTO")
     * Null per operazioni void o con tipi primitivi
     */
    private final String dataClassName;
    
    /**
     * Categoria del wrapper response (SINGLE, LIST, PAGE, VOID)
     */
    private final WrapperCategory wrapperCategory;
    
    /**
     * Riferimento allo schema di successo generato
     * (es: "#/components/schemas/ResponseWrapperRestaurantDTO")
     */
    private String successSchemaRef;
    
    /**
     * Riferimento allo schema di errore
     * (es: "#/components/schemas/ResponseWrapperErrorDetails")
     */
    private String errorSchemaRef;
    
    /**
     * Indica se gli schemi sono stati già generati
     */
    private boolean schemasGenerated;
    
    /**
     * Aggiorna i riferimenti agli schemi dopo la generazione
     */
    public void updateSchemaRefs(String successRef, String errorRef) {
        this.successSchemaRef = successRef;
        this.errorSchemaRef = errorRef;
        this.schemasGenerated = true;
    }
}
