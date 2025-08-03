package com.application.common.web;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ResponseWrapper", description = "Standard API response wrapper for single objects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    @Builder.Default
    private boolean success = true;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Error details (present only if success = false)")
    private ErrorDetails error;

    @Schema(description = "Response timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Success responses
    public static <T> ResponseWrapper<T> success(T data) {
        return ResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message("Success")
                .build();
    }

    public static <T> ResponseWrapper<T> success(T data, String message) {
        return ResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    // Created response (201)
    public static <T> ResponseWrapper<T> created(T data, String message) {
        return ResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    // Error responses
    public static <T> ResponseWrapper<T> error(String message) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> badRequest(String message, String errorCode) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> notFound(String message) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code("NOT_FOUND")
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> conflict(String message) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code("CONFLICT")
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> unauthorized(String message) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code("UNAUTHORIZED")
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> forbidden(String message) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code("FORBIDDEN")
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> internalServerError(String message) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message(message)
                        .build())
                .build();
    }

    // Error responses with custom status codes
    public static <T> ResponseWrapper<T> errorWithStatus(int statusCode, String message, String errorCode) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> error(String message, String code) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    public static <T> ResponseWrapper<T> error(ErrorDetails error) {
        return ResponseWrapper.<T>builder()
                .success(false)
                .message(error.getMessage())
                .error(error)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        @Schema(description = "Error code", example = "VALIDATION_ERROR")
        private String code;
        
        @Schema(description = "Error message", example = "Invalid input data")
        private String message;
        
        @Schema(description = "Field-specific errors")
        private Object details;
    }
}
