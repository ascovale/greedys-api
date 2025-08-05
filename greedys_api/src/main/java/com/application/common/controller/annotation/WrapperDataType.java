package com.application.common.controller.annotation;

/**
 * Enum that defines the wrapper data types for OpenAPI schema generation
 */
public enum WrapperDataType {
    /**
     * Single object - generates ResponseWrapper<T> schema
     */
    DTO,
    
    /**
     * List of objects - generates ListResponseWrapper<T> schema  
     */
    LIST,
    
    /**
     * Paginated objects - generates PageResponseWrapper<T> schema
     */
    PAGE
}
