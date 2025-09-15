package com.application.common.spring.swagger.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.metadata.WrapperCategory;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility per analizzare le operazioni e determinare i tipi di schemi da generare.
 * 
 * Analizza tutti i metadati delle operazioni e identifica:
 * - Schemi wrapper da generare (ResponseWrapperXXX)
 * - Schemi DTO da estrarre
 * - Categorie di wrapper utilizzate
 */
@Slf4j
public class SchemaTypeAnalyzer {
    
    /**
     * Risultato dell'analisi dei tipi di schemi
     */
    public static class SchemaAnalysisResult {
        private final Set<String> wrapperSchemasToGenerate;
        private final Set<String> dtoSchemasToExtract;
        private final Set<WrapperCategory> usedCategories;
        private final Set<String> dataClassNames;
        private final Map<String, String> wrapperToDataClassMapping;
    private final Map<String, WrapperCategory> wrapperToCategoryMapping;
        
    public SchemaAnalysisResult(Set<String> wrapperSchemasToGenerate, 
                   Set<String> dtoSchemasToExtract,
                   Set<WrapperCategory> usedCategories,
                   Set<String> dataClassNames,
                   Map<String, String> wrapperToDataClassMapping,
                   Map<String, WrapperCategory> wrapperToCategoryMapping) {
        this.wrapperSchemasToGenerate = wrapperSchemasToGenerate;
        this.dtoSchemasToExtract = dtoSchemasToExtract;
        this.usedCategories = usedCategories;
        this.dataClassNames = dataClassNames;
        this.wrapperToDataClassMapping = wrapperToDataClassMapping;
        this.wrapperToCategoryMapping = wrapperToCategoryMapping;
        }
        
        public Set<String> getWrapperSchemasToGenerate() { return wrapperSchemasToGenerate; }
        public Set<String> getDtoSchemasToExtract() { return dtoSchemasToExtract; }
        public Set<WrapperCategory> getUsedCategories() { return usedCategories; }
        public Set<String> getDataClassNames() { return dataClassNames; }
        public Map<String, String> getWrapperToDataClassMapping() { return wrapperToDataClassMapping; }
    public Map<String, WrapperCategory> getWrapperToCategoryMapping() { return wrapperToCategoryMapping; }
        
        public int getTotalSchemaCount() {
            return wrapperSchemasToGenerate.size() + dtoSchemasToExtract.size();
        }
        
        public String getSummary() {
            return String.format("SchemaAnalysis[wrappers=%d, dtos=%d, categories=%d]",
                wrapperSchemasToGenerate.size(), dtoSchemasToExtract.size(), usedCategories.size());
        }
    }
    
    /**
     * Analizza tutte le operazioni per determinare i tipi di schemi da generare
     */
    public static SchemaAnalysisResult analyzeSchemaTypes(Iterable<OperationDataMetadata> operations) {
        Set<String> wrapperSchemasToGenerate = new HashSet<>();
        Set<String> dtoSchemasToExtract = new HashSet<>();
        Set<WrapperCategory> usedCategories = new HashSet<>();
        Set<String> dataClassNames = new HashSet<>();
        Map<String, String> wrapperToDataClassMapping = new HashMap<>();
        
        Map<String, WrapperCategory> wrapperToCategoryMapping = new HashMap<>();
        for (OperationDataMetadata operation : operations) {
            analyzeOperation(operation, wrapperSchemasToGenerate, dtoSchemasToExtract, 
                           usedCategories, dataClassNames, wrapperToDataClassMapping, wrapperToCategoryMapping);
        }
        
        log.info("Schema analysis completed: {} wrapper schemas, {} DTO schemas, {} categories",
            wrapperSchemasToGenerate.size(), dtoSchemasToExtract.size(), usedCategories.size());
        
    return new SchemaAnalysisResult(wrapperSchemasToGenerate, dtoSchemasToExtract, 
                       usedCategories, dataClassNames, wrapperToDataClassMapping, wrapperToCategoryMapping);
    }
    
    /**
     * Analizza una singola operazione
     */
    private static void analyzeOperation(OperationDataMetadata operation,
                                       Set<String> wrapperSchemasToGenerate,
                                       Set<String> dtoSchemasToExtract,
                                       Set<WrapperCategory> usedCategories,
                                       Set<String> dataClassNames,
                                       Map<String, String> wrapperToDataClassMapping,
                                       Map<String, WrapperCategory> wrapperToCategoryMapping) {
        
        // 1. Categoria wrapper
        WrapperCategory category = operation.getWrapperCategory();
        if (category != null) {
            usedCategories.add(category);
            
            // 2. Nome schema wrapper da generare
            String dataClassName = operation.getDataClassName();
            String simpleClassName;
            String wrapperSchemaName;
            
            if (dataClassName != null && !dataClassName.trim().isEmpty()) {
                // Caso normale: abbiamo un dataClassName specifico
                simpleClassName = extractSimpleClassName(dataClassName);
                wrapperSchemaName = category.generateSchemaName(simpleClassName);
                wrapperSchemasToGenerate.add(wrapperSchemaName);
                
                // 3. Mappa wrapper -> classe dati per efficienza
                wrapperToDataClassMapping.put(wrapperSchemaName, dataClassName);
                // 3b. Mappa wrapper -> categoria
                wrapperToCategoryMapping.put(wrapperSchemaName, category);
                
                // 4. DTO da estrarre (se non è primitivo)
                if (!isPrimitiveType(simpleClassName)) {
                    dtoSchemasToExtract.add(dataClassName);
                    dataClassNames.add(simpleClassName);
                }
            } else {
                // Caso edge: dataClassName è null o vuoto, usa "String" come default
                simpleClassName = "String";
                wrapperSchemaName = category.generateSchemaName(simpleClassName);
                wrapperSchemasToGenerate.add(wrapperSchemaName);
                
                // 5. Mappa wrapper -> classe dati (null per indicare tipo primitivo default)
                wrapperToDataClassMapping.put(wrapperSchemaName, "java.lang.String");
                // 5b. Mappa wrapper -> categoria
                wrapperToCategoryMapping.put(wrapperSchemaName, category);
                
                log.debug("Operation {} has null/empty dataClassName, using String as default", 
                    operation.getOperationId());
            }
        }
        
        log.debug("Analyzed operation {}: category={}, dataClass={}", 
            operation.getOperationId(), category, operation.getDataClassName());
    }
    
    /**
     * Estrae il nome semplice della classe
     */
    private static String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) return "String";
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
    
    /**
     * Verifica se è un tipo primitivo/wrapper
     */
    private static boolean isPrimitiveType(String className) {
        return className.equals("String") || 
               className.equals("Integer") || 
               className.equals("Long") || 
               className.equals("Boolean") || 
               className.equals("Double") || 
               className.equals("Float") ||
               className.equals("Object") ||
               className.equals("Void");
    }
}
