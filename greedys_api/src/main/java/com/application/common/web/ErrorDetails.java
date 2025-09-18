package com.application.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ErrorDetails", description = "Error details for API responses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Field-specific errors or additional details")
    private Object details;

    // Factory methods for convenience
    public static ErrorDetails of(String code) {
        return ErrorDetails.builder()
                .code(code)
                .build();
    }

    public static ErrorDetails of(String code, Object details) {
        return ErrorDetails.builder()
                .code(code)
                .details(details)
                .build();
    }
}
