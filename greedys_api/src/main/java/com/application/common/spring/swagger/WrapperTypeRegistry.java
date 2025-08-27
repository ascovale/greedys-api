package com.application.common.spring.swagger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry for collecting and managing all unique wrapper types discovered from API operations.
 * 
 * This class is responsible for scanning the OpenAPI specification and extracting
 * wrapper type information that was previously added by WrapperTypeOperationCustomizer.
 * It maintains registries of processed types and their schema names to prevent duplicates.
 */
@Slf4j
public class WrapperTypeRegistry {
    
    private final String apiGroup;
    
    // Registry of data types that have been processed (T in wrapper<T>)
    private final Set<String> registeredDataTypes = new HashSet<>();
    
    // Mapping from "dataClass:wrapperType" to generated schema name
    private final Map<String, String> wrapperSchemaNames = new HashMap<>();
    
    // Direct storage for wrapper type information (instead of using extensions)
    private final Set<WrapperTypeInfo> directWrapperTypes = new HashSet<>();
    
    // Optimized: Direct mapping from "dataClass:wrapperType" to complete schema reference
    private final Map<String, String> wrapperSchemaReferences = new HashMap<>();
    
    // NEW: Success responses per operation - DOPPIA MAPPA per supportare entrambe le chiavi
    private final Map<String, List<ResponseInfo>> operationSuccessResponses = new HashMap<>(); // Chiave: methodSignature
    private final Map<String, List<ResponseInfo>> operationIdSuccessResponses = new HashMap<>(); // Chiave: operationId
    
    /**
     * Constructor with API group name for logging and identification
     */
    public WrapperTypeRegistry(String apiGroup) {
        this.apiGroup = apiGroup;
        log.warn("🔍 REGISTRY LIFECYCLE: Created WrapperTypeRegistry for API group: {}, hashCode={}", apiGroup, this.hashCode());
    }

    /**
     * Clear all registries for fresh processing
     * 
     * Called at the beginning of customization to ensure clean state
     */
    public void clearRegistries() {
        log.warn("🔍 REGISTRY LIFECYCLE: clearRegistries() CALLED! - before: size={}, hashCode={}", 
            directWrapperTypes.size(), this.hashCode());
        
        // Stampo lo stack trace per vedere CHI sta chiamando clearRegistries
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.warn("🔍 REGISTRY LIFECYCLE: clearRegistries() called from:");
        for (int i = 1; i < Math.min(stackTrace.length, 6); i++) {
            log.warn("🔍   at {}", stackTrace[i]);
        }
        
        registeredDataTypes.clear();
        wrapperSchemaNames.clear();
        directWrapperTypes.clear();
        wrapperSchemaReferences.clear();
        operationSuccessResponses.clear(); // Clear success responses by methodSignature
        operationIdSuccessResponses.clear(); // Clear success responses by operationId
        
        log.warn("🔍 REGISTRY LIFECYCLE: clearRegistries() COMPLETED! - after: size={}", directWrapperTypes.size());
    }

    /**
     * Register wrapper type information directly (without using OpenAPI extensions)
     * 
     * @param dataClass Full class name of the data type
     * @param wrapperType Type of wrapper (DTO, LIST, PAGE)
     */
    public void registerDirectWrapperType(String dataClass, String wrapperType) {
        WrapperTypeInfo typeInfo = new WrapperTypeInfo(dataClass, wrapperType, "Auto-detected");
        boolean isNew = directWrapperTypes.add(typeInfo);
        
        // Aggiungo logging per tracciare le registrazioni
        // log.warn("🔍 REGISTRY LIFECYCLE: registerDirectWrapperType() - {}, isNew={}, size={}, hashCode={}", 
        //     typeInfo.getWrapperSchemaName(), isNew, directWrapperTypes.size(), this.hashCode());
        
        // 🎯 TRACE SPECIFICO PER AuthResponseDTO
        if (dataClass.contains("AuthResponseDTO")) {
            log.warn("🎯 FASE1-AuthResponseDTO: REGISTERED in registry! schema={}, isNew={}, size={}", 
                typeInfo.getWrapperSchemaName(), isNew, directWrapperTypes.size());
        }
        
        if (isNew) {
            // Store the complete schema reference for fast FASE 5 lookup
            String key = dataClass + ":" + wrapperType;
            String schemaReference = typeInfo.getWrapperSchemaReference();
            wrapperSchemaReferences.put(key, schemaReference);
            
            // log.warn("⚠️ REGISTRY: Registrato {} -> {}", typeInfo.getWrapperSchemaName(), schemaReference);
        } else {
            // log.warn("⚠️ REGISTRY: Già esistente {}", typeInfo.getWrapperSchemaName());
        }
    }

    /**
     * Get all wrapper types (combination of direct registrations and extension scanning)
     * 
     * @param openApi OpenAPI specification to scan for fallback
     * @return Set of unique WrapperTypeInfo objects
     */
    public Set<WrapperTypeInfo> collectAllWrapperTypes(OpenAPI openApi) {
        Set<WrapperTypeInfo> wrapperTypes = new HashSet<>();
        
        // First, add all directly registered wrapper types
        wrapperTypes.addAll(directWrapperTypes);
        // log.warn("⚠️ REGISTRY: Trovati {} wrapper types registrati (directWrapperTypes.size={})", 
        //     directWrapperTypes.size(), directWrapperTypes.size());
        
        // 🎯 TRACE SPECIFICO PER AuthResponseDTO
        for (WrapperTypeInfo info : directWrapperTypes) {
            if (info.dataClassName.contains("AuthResponseDTO")) {
                log.warn("🎯 FASE2-AuthResponseDTO: FOUND in directWrapperTypes! schema={}", 
                    info.getWrapperSchemaName());
            }
        }
        
        // Aggiungo logging per tracciare il ciclo di vita del Registry
        // log.warn("🔍 REGISTRY LIFECYCLE: collectAllWrapperTypes() - directWrapperTypes.size={}, hashCode={}", 
        //     directWrapperTypes.size(), this.hashCode());
        
        // for (WrapperTypeInfo info : directWrapperTypes) {
        //     log.warn("⚠️ REGISTRY: - {} ({})", info.getWrapperSchemaName(), info.wrapperType);
        // }
        
        // Fallback: scan for extension-based wrapper types if no direct registrations
        if (directWrapperTypes.isEmpty() && openApi.getPaths() != null) {
            // log.warn("⚠️ REGISTRY: directWrapperTypes è vuoto, fallback scan...");
            openApi.getPaths().forEach((path, pathItem) -> {
                collectWrapperTypesFromPathItem(path, pathItem, wrapperTypes);
            });
        }
        
        System.out.println("WrapperTypeRegistry: Total wrapper types collected: " + wrapperTypes.size() + " (direct: " + directWrapperTypes.size() + ")");
        return wrapperTypes;
    }
    
    /**
     * Extract wrapper type information from all HTTP operations in a single path
     * 
     * @param path API path being processed (for logging)
     * @param pathItem PathItem containing HTTP operations
     * @param wrapperTypes Collection to add discovered wrapper types to
     */
    private void collectWrapperTypesFromPathItem(String path, PathItem pathItem, Set<WrapperTypeInfo> wrapperTypes) {
        int initialSize = wrapperTypes.size();
        
        collectWrapperTypesFromOperation(pathItem.getGet(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getPost(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getPut(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getDelete(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getPatch(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getHead(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getOptions(), wrapperTypes);
        collectWrapperTypesFromOperation(pathItem.getTrace(), wrapperTypes);
        
        int addedTypes = wrapperTypes.size() - initialSize;
        if (addedTypes > 0) {
            System.out.println("WrapperTypeRegistry: Path " + path + " contributed " + addedTypes + " wrapper types");
        }
    }
    
    /**
     * Extract wrapper type information from a single operation's extensions
     * 
     * NOTA: Questo metodo ora è vuoto perché usiamo direttamente i directWrapperTypes
     * popolati da WrapperTypeOperationCustomizer in FASE 1. Non servono più le 
     * extension x-wrapper-type.
     * 
     * @param operation HTTP operation to examine  
     * @param wrapperTypes Collection to add discovered wrapper types to
     */
    private void collectWrapperTypesFromOperation(Operation operation, Set<WrapperTypeInfo> wrapperTypes) {
        // Non più necessario - usiamo directWrapperTypes popolati in FASE 1
    }

    /**
     * Register a data type as processed in the schema generation
     * 
     * @param dataClassName Full class name that has been processed
     */
    public void registerDataType(String dataClassName) {
        registeredDataTypes.add(dataClassName);
        System.out.println("WrapperTypeRegistry: Registered data type " + getSimpleClassName(dataClassName));
    }

    /**
     * Register a generated wrapper schema name for future reference
     * 
     * @param dataClassName Full class name of the data type
     * @param wrapperType Type of wrapper (ResponseWrapper, List, Page)
     * @param schemaName Generated schema name in components/schemas
     */
    public void registerWrapperSchema(String dataClassName, String wrapperType, String schemaName) {
        String key = dataClassName + ":" + wrapperType;
        wrapperSchemaNames.put(key, schemaName);
        System.out.println("WrapperTypeRegistry: Registered wrapper schema " + schemaName + " for " + 
                         wrapperType + "<" + getSimpleClassName(dataClassName) + ">");
    }

    /**
     * Check if a data type has already been processed
     * 
     * @param dataClassName Full class name to check
     * @return true if already processed, false otherwise
     */
    public boolean isDataTypeRegistered(String dataClassName) {
        return registeredDataTypes.contains(dataClassName);
    }

    /**
     * Get the schema name for a specific wrapper type combination
     * 
     * @param dataClassName Full class name of the data type
     * @param wrapperType Type of wrapper (ResponseWrapper, List, Page)
     * @return Schema name in components/schemas, or null if not found
     */
    public String getWrapperSchemaName(String dataClassName, String wrapperType) {
        String key = dataClassName + ":" + wrapperType;
        return wrapperSchemaNames.get(key);
    }
    
    /**
     * Register success responses for an operation (NEW)
     * 
     * @param methodSignature Signature del metodo (es: "UserController.getUser")
     * @param responses Lista delle success responses (200, 201) per l'operation
     */
    public void registerSuccessResponses(String methodSignature, List<ResponseInfo> responses) {
        if (responses != null && !responses.isEmpty()) {
            operationSuccessResponses.put(methodSignature, responses);
            
            log.debug("🔍 REGISTRY: Registered {} success responses for operation: {}", 
                responses.size(), methodSignature);
        }
    }

    /**
     * Register success responses for an operation by operationId (NEW)
     * 
     * @param operationId ID dell'operazione OpenAPI (es: "getTables")
     * @param responses Lista delle success responses (200, 201) per l'operation
     */
    public void registerSuccessResponsesByOperationId(String operationId, List<ResponseInfo> responses) {
        if (responses != null && !responses.isEmpty()) {
            operationIdSuccessResponses.put(operationId, responses);
            
            log.debug("🔍 REGISTRY: Registered {} success responses for operationId: {}", 
                responses.size(), operationId);
        }
    }

    /**
     * Get success responses for an operation (NEW)
     * 
     * @param methodSignature Signature del metodo
     * @return Lista delle success responses registrate, o lista vuota se non trovate
     */
    public List<ResponseInfo> getSuccessResponses(String methodSignature) {
        return operationSuccessResponses.getOrDefault(methodSignature, new java.util.ArrayList<>());
    }

    /**
     * Get success responses for an operation by operationId (NEW)
     * 
     * @param operationId ID dell'operazione OpenAPI
     * @return Lista delle success responses registrate, o lista vuota se non trovate
     */
    public List<ResponseInfo> getSuccessResponsesByOperationId(String operationId) {
        return operationIdSuccessResponses.getOrDefault(operationId, new java.util.ArrayList<>());
    }

    /**
     * Get all registered operation signatures with success responses (NEW)
     * 
     * @return Set di tutte le method signature registrate
     */
    public Set<String> getRegisteredOperations() {
        return new HashSet<>(operationSuccessResponses.keySet());
    }

    /**
     * Get the complete schema reference for a specific wrapper type combination
     * This is optimized for FASE 5 - eliminates need for x-wrapper-type extensions
     * 
     * @param dataClassName Full class name of the data type
     * @param wrapperType Type of wrapper (DTO, LIST, PAGE)
     * @return Complete schema reference like "#/components/schemas/ResponseWrapperUserDto", or null if not found
     */
    public String getWrapperSchemaReference(String dataClassName, String wrapperType) {
        String key = dataClassName + ":" + wrapperType;
        return wrapperSchemaReferences.get(key);
    }

    /**
     * Get all registered data types for validation
     * 
     * @return Copy of all registered data type class names
     */
    public Set<String> getRegisteredDataTypes() {
        return new HashSet<>(registeredDataTypes);
    }

    /**
     * Get all registered wrapper schema mappings for validation
     * 
     * @return Copy of all wrapper schema name mappings
     */
    public Map<String, String> getWrapperSchemaNames() {
        return new HashMap<>(wrapperSchemaNames);
    }

    /**
     * Get statistics about the registry state
     * 
     * @return Human-readable summary of registry contents
     */
    public String getRegistryStats() {
        return String.format("Registry stats: %d data types, %d wrapper schemas", 
                           registeredDataTypes.size(), wrapperSchemaNames.size());
    }

    /**
     * Extract simple class name from full class name for logging
     * 
     * @param fullClassName Full Java class name
     * @return Simple class name (last part after dot)
     */
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "null";
        }
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }
}
