package com.application.common.spring.swagger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Informazioni su una response HTTP per OpenAPI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ResponseInfo {
    
    /**
     * HTTP status code (es: "200", "201", "400")
     */
    private String code;
    
    /**
     * Descrizione della response
     */
    private String description;
    
    /**
     * Riferimento allo schema wrapper (es: "#/components/schemas/ResponseWrapperUserDto")
     */
    private String wrapperSchemaRef;
    
    /**
     * Tipo di wrapper (DTO, LIST, PAGE)
     */
    private String wrapperType;
    
    /**
     * Indica se è una success response (2xx) o error response (4xx, 5xx)
     */
    private boolean isSuccess;
    
    /**
     * Factory method per success response
     */
    public static ResponseInfo success(String code, String description, String wrapperSchemaRef, String wrapperType) {
        return ResponseInfo.builder()
            .code(code)
            .description(description)
            .wrapperSchemaRef(wrapperSchemaRef)
            .wrapperType(wrapperType)
            .isSuccess(true)
            .build();
    }
    
    /**
     * Factory method per error response
     */
    public static ResponseInfo error(String code, String description, String wrapperSchemaRef) {
        return ResponseInfo.builder()
            .code(code)
            .description(description)
            .wrapperSchemaRef(wrapperSchemaRef)
            .wrapperType("ERROR")
            .isSuccess(false)
            .build();
    }
}
