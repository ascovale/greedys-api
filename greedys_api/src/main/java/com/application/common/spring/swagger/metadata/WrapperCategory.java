package com.application.common.spring.swagger.metadata;

/**
 * Categoria del wrapper type per le API responses.
 * 
 * Sostituisce il vecchio sistema di stringhe "DTO", "LIST", "PAGE" con enum più chiaro.
 * Ogni categoria corrisponde a un pattern specifico di ResponseWrapper.
 */
public enum WrapperCategory {
    
    /**
     * ResponseWrapper<T> - Single object response
     * Esempio: ResponseWrapper<UserDto>
     */
    SINGLE,
    
    /**
     * ResponseWrapper<List<T>> - List response  
     * Esempio: ResponseWrapper<List<UserDto>>
     */
    LIST,
    
    /**
     * ResponseWrapper<Page<T>> - Paginated response
     * Esempio: ResponseWrapper<Page<UserDto>>
     */
    PAGE,
    
    /**
     * ResponseWrapper<String> - Void operations response
     * Esempio: executeVoid() operations
     */
    VOID;
    
    /**
     * Convert from legacy string format to enum
     * 
     * @param legacyType Old string format ("DTO", "LIST", "PAGE")
     * @return Corresponding WrapperCategory
     */
    public static WrapperCategory fromLegacyString(String legacyType) {
        if (legacyType == null) {
            return SINGLE; // default fallback
        }
        
        switch (legacyType.toUpperCase()) {
            case "DTO":
                return SINGLE;
            case "LIST":
                return LIST;
            case "PAGE":
                return PAGE;
            case "VOID":
            case "STRING":
                return VOID;
            default:
                return SINGLE; // fallback
        }
    }
    
    /**
     * Get schema name pattern for this category
     * 
     * @return Pattern string with %s placeholder for data type name
     */
    public String getSchemaPattern() {
        switch (this) {
            case SINGLE:
                return "ResponseWrapper%s";
            case LIST:
                return "ResponseWrapperList%s";
            case PAGE:
                return "ResponseWrapperPage%s";
            case VOID:
                return "ResponseWrapperString";
            default:
                return "ResponseWrapper%s";
        }
    }
    
    /**
     * Generate concrete schema name for this category and data type
     * 
     * @param dataTypeSimpleName Simple class name (e.g., "UserDto")
     * @return Concrete schema name (e.g., "ResponseWrapperUserDto")
     */
    public String generateSchemaName(String dataTypeSimpleName) {
        if (this == VOID) {
            return "ResponseWrapperString";
        }
        
        String pattern = getSchemaPattern();
        return String.format(pattern, dataTypeSimpleName != null ? dataTypeSimpleName : "");
    }
    
    /**
     * Generate schema reference for OpenAPI
     * 
     * @param dataTypeSimpleName Simple class name
     * @return Full schema reference (e.g., "#/components/schemas/ResponseWrapperUserDto")
     */
    public String generateSchemaReference(String dataTypeSimpleName) {
        return "#/components/schemas/" + generateSchemaName(dataTypeSimpleName);
    }
}
