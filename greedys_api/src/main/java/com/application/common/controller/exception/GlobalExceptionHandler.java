package com.application.common.controller.exception;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.application.common.web.ApiResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private void logPotentialBaseControllerMiss(String exceptionType, Exception ex, WebRequest request) {
        String requestUri = request.getDescription(false);
        log.warn("⚠️  POTENTIAL BaseController MISS: {} caught by GlobalExceptionHandler instead of BaseController. " +
                "Request: {}, Exception: {}", 
                exceptionType, requestUri, ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<String>> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        logPotentialBaseControllerMiss("NoSuchElementException", ex, request);
        log.warn("Resource not found: ", ex);
        ApiResponse<String> response = ApiResponse.error("Resource not found", "RESOURCE_NOT_FOUND");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logPotentialBaseControllerMiss("IllegalArgumentException", ex, request);
        log.warn("Invalid request: ", ex);
        ApiResponse<String> response = ApiResponse.error("Invalid request: " + ex.getMessage(), "INVALID_REQUEST");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: ", ex);
        ApiResponse<String> response = ApiResponse.error("HTTP method not supported for this endpoint", "METHOD_NOT_ALLOWED");
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request or invalid format: ", ex);
        ApiResponse<String> response = ApiResponse.error("Unreadable request or invalid format", "INVALID_FORMAT");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing parameter: ", ex);
        ApiResponse<String> response = ApiResponse.error("Missing parameter: " + ex.getParameterName(), "MISSING_PARAMETER");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        logPotentialBaseControllerMiss("ConstraintViolationException", ex, request);
        log.warn("Validation error: ", ex);
        ApiResponse<String> response = ApiResponse.error("Validation error: " + ex.getMessage(), "VALIDATION_ERROR");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        logPotentialBaseControllerMiss("DataIntegrityViolationException", ex, request);
        log.error("Data integrity violation: ", ex);
        ApiResponse<String> response = ApiResponse.error("Data integrity violation", "DATA_INTEGRITY_VIOLATION");
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        log.error("Authentication failed: ", ex);
        ApiResponse<String> response = ApiResponse.error("Authentication failed: invalid credentials", "AUTHENTICATION_FAILED");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: ", ex);
        ApiResponse<String> response = ApiResponse.error("Access denied: you do not have the necessary privileges", "ACCESS_DENIED");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ApiResponse<String>> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex, WebRequest request) {
        log.error("Authentication service error: ", ex);
        ApiResponse<String> response = ApiResponse.error("Authentication service error", "AUTHENTICATION_SERVICE_ERROR");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ApiResponse.ErrorDetails>>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        logPotentialBaseControllerMiss("MethodArgumentNotValidException", ex, request);
        
        List<ApiResponse.ErrorDetails> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ApiResponse.ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .message(error.getField() + ": " + error.getDefaultMessage())
                        .details(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        String mainMessage = fieldErrors.isEmpty() ? "Validation error" : fieldErrors.get(0).getMessage();
        
        ApiResponse<List<ApiResponse.ErrorDetails>> response = ApiResponse.<List<ApiResponse.ErrorDetails>>builder()
                .success(false)
                .message("Validation failed")
                .data(fieldErrors)
                .error(ApiResponse.ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .message(mainMessage)
                        .details(fieldErrors)
                        .build())
                .build();
                
        log.warn("Validation error: {}", mainMessage, ex);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex, WebRequest request) {
        log.error("🔴 UNHANDLED EXCEPTION in GlobalExceptionHandler - This should potentially be handled by BaseController: {} - Request: {}", 
                ex.getClass().getSimpleName(), request.getDescription(false), ex);
        ApiResponse<String> response = ApiResponse.error("Internal server error", "INTERNAL_ERROR");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
