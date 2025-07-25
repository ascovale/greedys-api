package com.application.common.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Composite annotation for READ operation responses
 * Includes 200 OK, 404 Not Found + standard error responses
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200", 
        description = "Resource found and returned successfully",
        content = @Content(schema = @Schema(implementation = com.application.common.web.ApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "400", 
        description = "Bad Request - Invalid input parameters",
        content = @Content(schema = @Schema(implementation = com.application.common.web.ApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "401", 
        description = "Unauthorized - Authentication required",
        content = @Content(schema = @Schema(implementation = com.application.common.web.ApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "403", 
        description = "Forbidden - Access denied",
        content = @Content(schema = @Schema(implementation = com.application.common.web.ApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "404", 
        description = "Not Found - Resource not found",
        content = @Content(schema = @Schema(implementation = com.application.common.web.ApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "500", 
        description = "Internal Server Error",
        content = @Content(schema = @Schema(implementation = com.application.common.web.ApiResponse.class))
    )
})
public @interface ReadApiResponses {
}
