package com.application.common.spring.swagger;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Risultato dell'analisi statica di un metodo controller
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationAnalysis {
    
    /**
     * Tipo di flusso rilevato dal pattern execute*()
     */
    private ReturnTypeFlow returnTypeFlow;
    
    /**
     * HTTP method rilevato dalle annotazioni (@GetMapping, @PostMapping, etc.)
     */
    private String httpMethod;
    
    /**
     * Informazioni sul wrapper type rilevato dalla method signature
     */
    private WrapperTypeInfo wrapperTypeInfo;
    
    /**
     * Lista delle success responses inferite (200, 201)
     */
    private List<ResponseInfo> successResponses;
    
    /**
     * Lista delle error responses dalle annotazioni
     */
    private List<ResponseInfo> errorResponses;
    
    /**
     * Signature del metodo per identificazione univoca
     */
    private String methodSignature;
    
    /**
     * Indica se l'analisi è stata completata con successo
     */
    private boolean analysisComplete;
    
    /**
     * Eventuali note o warning dall'analisi
     */
    private String notes;
}
