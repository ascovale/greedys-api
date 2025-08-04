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

    @Schema(description = "Error message", example = "Invalid input data")
    private String message;

    @Schema(description = "Field-specific errors")
    private Object details;

    // Factory methods for convenience
    public static ErrorDetails of(String code, String message) {
        return ErrorDetails.builder()
                .code(code)
                .message(message)
                .build();
    }

    public static ErrorDetails of(String code, String message, Object details) {
        return ErrorDetails.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();
    }
}
