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
     * List of objects - generates ResponseWrapper<List<T>> schema with ListMetadata
     */
    LIST,
    
    /**
     * Paginated objects - generates ResponseWrapper<Page<T>> schema with PageMetadata
     */
    PAGE
}
