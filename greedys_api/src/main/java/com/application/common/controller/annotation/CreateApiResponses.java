package com.application.common.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)

@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Resource created successfully"),
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input parameters"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
    @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
    @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
})
public @interface CreateApiResponses {
}
