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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.application.common.web.ErrorDetails;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;

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
    public ResponseEntity<ResponseWrapper<String>> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        logPotentialBaseControllerMiss("NoSuchElementException", ex, request);
        log.warn("Resource not found: ", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error("Resource not found", "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logPotentialBaseControllerMiss("IllegalArgumentException", ex, request);
        log.warn("Invalid request: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Invalid request: " + ex.getMessage(), "INVALID_REQUEST"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        logPotentialBaseControllerMiss("HttpRequestMethodNotSupportedException", ex, request);
        log.warn("HTTP method not supported: ", ex);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseWrapper.<String>error("HTTP method not supported for this endpoint", "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseWrapper<String>> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        logPotentialBaseControllerMiss("HttpMessageNotReadableException", ex, request);
        log.warn("Unreadable request or invalid format: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Unreadable request or invalid format", "INVALID_FORMAT"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        logPotentialBaseControllerMiss("MissingServletRequestParameterException", ex, request);
        log.warn("Missing parameter: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Missing parameter: " + ex.getParameterName(), "MISSING_PARAMETER"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseWrapper<String>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        logPotentialBaseControllerMiss("ConstraintViolationException", ex, request);
        log.warn("Validation error: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Validation error: " + ex.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseWrapper<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        logPotentialBaseControllerMiss("DataIntegrityViolationException", ex, request);
        log.error("Data integrity violation: ", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.<String>error("Data integrity violation", "DATA_INTEGRITY_VIOLATION"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        logPotentialBaseControllerMiss("UsernameNotFoundException", ex, request);
        log.error("Authentication failed: ", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Authentication failed: invalid credentials", "AUTHENTICATION_FAILED"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logPotentialBaseControllerMiss("AccessDeniedException", ex, request);
        log.warn("Access denied: ", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseWrapper.<String>error("Access denied: you do not have the necessary privileges", "ACCESS_DENIED"));
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ResponseWrapper<String>> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex, WebRequest request) {
        logPotentialBaseControllerMiss("InternalAuthenticationServiceException", ex, request);
        log.error("Authentication service error: ", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Authentication service error", "AUTHENTICATION_SERVICE_ERROR"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
        String requestUri = request.getDescription(false);
        String resourcePath = ex.getResourcePath();
        
        // Per favicon e altre risorse statiche, non loggare come errori - è comportamento normale del browser
        if (resourcePath != null && (resourcePath.contains("favicon") || 
                                   resourcePath.endsWith(".ico") || 
                                   resourcePath.endsWith(".png") || 
                                   resourcePath.endsWith(".jpg") || 
                                   resourcePath.endsWith(".css") || 
                                   resourcePath.endsWith(".js"))) {
            log.debug("Static resource not found (normal browser behavior): {}", resourcePath);
        } else {
            log.warn("Resource not found: {}", requestUri);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error("Resource not found", "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ListResponseWrapper<ErrorDetails>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        logPotentialBaseControllerMiss("MethodArgumentNotValidException", ex, request);
        
        List<ErrorDetails> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .message(error.getField() + ": " + error.getDefaultMessage())
                        .details(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        String mainMessage = fieldErrors.isEmpty() ? "Validation error" : fieldErrors.get(0).getMessage();
        
        log.warn("Validation error: {}", mainMessage, ex);
        
        ListResponseWrapper<ErrorDetails> response = ListResponseWrapper.error("Validation failed", ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .message(mainMessage)
                .details(fieldErrors)
                .build());
        response.setData(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<String>> handleGenericException(Exception ex, WebRequest request) {
        log.error("🔴 UNHANDLED EXCEPTION in GlobalExceptionHandler - This should potentially be handled by BaseController: {} - Request: {}", 
                ex.getClass().getSimpleName(), request.getDescription(false), ex);
        logPotentialBaseControllerMiss("Exception", ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.<String>error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }

}
