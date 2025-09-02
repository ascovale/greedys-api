package com.application.common.spring.swagger.analysis;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * Risultato dell'analisi di un metodo del controller.
 * 
 * Contiene tutti i metadati estratti dall'analisi:
 * - Status codes (principale + errori standard)
 * - Signature del metodo 
 * - Note di analisi per debugging
 */
@Data
@Builder
public class MethodAnalysisResult {
    
    /**
     * Status codes per questo metodo.
     * Include il codice principale (200/201) + errori standard (400,401,403,500)
     */
    private final Set<String> statusCodes;
    
    /**
     * Signature del metodo per debugging
     * Es: "CustomerController.createCustomer(3 params)"
     */
    private final String methodSignature;
    
    /**
     * Note di analisi per debugging
     * Es: "Successful analysis", "Analysis failed: ..."
     */
    private final String analysisNotes;
    
    /**
     * Crea un risultato di default per casi di errore
     */
    public static MethodAnalysisResult createDefault() {
        return MethodAnalysisResult.builder()
            .statusCodes(Set.of("200", "400", "401", "403", "500"))
            .methodSignature("unknown")
            .analysisNotes("Default analysis")
            .build();
    }
}
