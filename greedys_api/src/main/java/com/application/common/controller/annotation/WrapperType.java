package com.application.common.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Annotation to specify the wrapper type and data class for OpenAPI schema generation.
 * This annotation automatically generates the correct OpenAPI schema based on the wrapper type used.
 * 
 * Examples:
 * - @WrapperType(dataClass = UserDTO.class, type = WrapperDataType.DTO) 
 *   → generates ResponseWrapper<UserDTO> schema
 * - @WrapperType(dataClass = UserDTO.class, type = WrapperDataType.LIST) 
 *   → generates ListResponseWrapper<UserDTO> schema  
 * - @WrapperType(dataClass = UserDTO.class, type = WrapperDataType.PAGE) 
 *   → generates PageResponseWrapper<UserDTO> schema
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "200",
    description = "Successful operation",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = Object.class) // Will be dynamically set based on wrapper type
    )
)
public @interface WrapperType {
    
    /**
     * The data class type that will be wrapped
     * @return the class of the data object
     */
    Class<?> dataClass();
    
    /**
     * The wrapper type to use for the response
     * @return the wrapper data type (DTO, LIST, or PAGE)
     */
    WrapperDataType type() default WrapperDataType.LIST;
    
    /**
     * Optional description for the response
     * @return response description
     */
    String description() default "Successful operation";
}
