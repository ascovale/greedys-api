package com.application.common.spring.swagger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

/**
 * Registry for collecting and managing all unique wrapper types discovered from API operations.
 * 
 * This class is responsible for scanning the OpenAPI specification and extracting
 * wrapper type information that was previously added by WrapperTypeOperationCustomizer.
 * It maintains registries of processed types and their schema names to prevent duplicates.
 */
@Component
public class WrapperTypeRegistry {
    
    // Registry of data types that have been processed (T in wrapper<T>)
    private final Set<String> registeredDataTypes = new HashSet<>();
    
    // Mapping from "dataClass:wrapperType" to generated schema name
    private final Map<String, String> wrapperSchemaNames = new HashMap<>();
    
    // Direct storage for wrapper type information (instead of using extensions)
    private final Set<WrapperTypeInfo> directWrapperTypes = new HashSet<>();

    /**
     * Clear all registries for fresh processing
     * 
     * Called at the beginning of customization to ensure clean state
     */
    public void clearRegistries() {
        registeredDataTypes.clear();
        wrapperSchemaNames.clear();
        directWrapperTypes.clear();
        System.out.println("WrapperTypeRegistry: Cleared all registries for fresh processing");
    }

    /**
     * Register wrapper type information directly (without using OpenAPI extensions)
     * 
     * @param dataClass Full class name of the data type
     * @param wrapperType Type of wrapper (DTO, LIST, PAGE)
     * @param responseCode HTTP response code (200, 201, etc.)
     */
    public void registerDirectWrapperType(String dataClass, String wrapperType, String responseCode) {
        WrapperTypeInfo typeInfo = new WrapperTypeInfo(dataClass, wrapperType, Arrays.asList(responseCode), "Auto-detected");
        boolean isNew = directWrapperTypes.add(typeInfo);
        
        if (isNew) {
            System.out.println("WrapperTypeRegistry: Registered direct wrapper type " + wrapperType + "<" + 
                             getSimpleClassName(dataClass) + "> for response " + responseCode);
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
        System.out.println("WrapperTypeRegistry: Using " + directWrapperTypes.size() + " directly registered wrapper types");
        
        // Fallback: scan for extension-based wrapper types if no direct registrations
        if (directWrapperTypes.isEmpty() && openApi.getPaths() != null) {
            System.out.println("WrapperTypeRegistry: No direct registrations found, scanning " + openApi.getPaths().size() + " paths for extensions");
            
            openApi.getPaths().forEach((path, pathItem) -> {
                collectWrapperTypesFromPathItem(path, pathItem, wrapperTypes);
            });
        }
        
        System.out.println("WrapperTypeRegistry: Total wrapper types collected: " + wrapperTypes.size());
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
     * This method reads the 'x-wrapper-type' extension data that was previously
     * added by WrapperTypeOperationCustomizer during the operation customization phase.
     * 
     * Expected extension format:
     * {
     *   "x-wrapper-type": {
     *     "dataClass": "com.example.UserDto",
     *     "type": "ResponseWrapper",
     *     "responseCode": "200", 
     *     "description": "User retrieved successfully"
     *   }
     * }
     * 
     * @param operation HTTP operation to examine
     * @param wrapperTypes Collection to add discovered wrapper types to
     */
    private void collectWrapperTypesFromOperation(Operation operation, Set<WrapperTypeInfo> wrapperTypes) {
        if (operation == null || operation.getExtensions() == null) {
            return;
        }

        // Look for wrapper type metadata added by OperationCustomizer
        Object wrapperTypeData = operation.getExtensions().get("x-wrapper-type");
        if (wrapperTypeData instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> wrapperInfo = (Map<String, Object>) wrapperTypeData;
                
                // Extract wrapper type information
                String dataClassName = (String) wrapperInfo.get("dataClass");
                String wrapperType = (String) wrapperInfo.get("type");
                String description = (String) wrapperInfo.getOrDefault("description", "Successful operation");
                String responseCode = (String) wrapperInfo.getOrDefault("responseCode", "200");

                // Validate required fields and create wrapper type info
                if (dataClassName != null && wrapperType != null) {
                    WrapperTypeInfo typeInfo = new WrapperTypeInfo(dataClassName, wrapperType, Arrays.asList(responseCode), description);
                    boolean isNew = wrapperTypes.add(typeInfo);
                    
                    if (isNew) {
                        System.out.println("WrapperTypeRegistry: Discovered " + wrapperType + "<" + 
                                         getSimpleClassName(dataClassName) + "> for response " + responseCode);
                    }
                } else {
                    System.err.println("WrapperTypeRegistry: Invalid wrapper type data - missing dataClass or type: " + wrapperInfo);
                }
                
            } catch (ClassCastException e) {
                System.err.println("WrapperTypeRegistry: Invalid wrapper type extension format: " + wrapperTypeData);
            }
        }
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
