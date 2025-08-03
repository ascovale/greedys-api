package com.application.common.controller.exception;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseWrapper<String> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        logPotentialBaseControllerMiss("NoSuchElementException", ex, request);
        log.warn("Resource not found: ", ex);
        return ResponseWrapper.error("Resource not found", "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<String> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logPotentialBaseControllerMiss("IllegalArgumentException", ex, request);
        log.warn("Invalid request: ", ex);
        return ResponseWrapper.error("Invalid request: " + ex.getMessage(), "INVALID_REQUEST");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseWrapper<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: ", ex);
        return ResponseWrapper.error("HTTP method not supported for this endpoint", "METHOD_NOT_ALLOWED");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<String> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request or invalid format: ", ex);
        return ResponseWrapper.error("Unreadable request or invalid format", "INVALID_FORMAT");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<String> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing parameter: ", ex);
        return ResponseWrapper.error("Missing parameter: " + ex.getParameterName(), "MISSING_PARAMETER");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseWrapper<String> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        logPotentialBaseControllerMiss("ConstraintViolationException", ex, request);
        log.warn("Validation error: ", ex);
        return ResponseWrapper.error("Validation error: " + ex.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseWrapper<String> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        logPotentialBaseControllerMiss("DataIntegrityViolationException", ex, request);
        log.error("Data integrity violation: ", ex);
        return ResponseWrapper.error("Data integrity violation", "DATA_INTEGRITY_VIOLATION");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseWrapper<String> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        log.error("Authentication failed: ", ex);
        return ResponseWrapper.error("Authentication failed: invalid credentials", "AUTHENTICATION_FAILED");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseWrapper<String> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: ", ex);
        return ResponseWrapper.error("Access denied: you do not have the necessary privileges", "ACCESS_DENIED");
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseWrapper<String> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex, WebRequest request) {
        log.error("Authentication service error: ", ex);
        return ResponseWrapper.error("Authentication service error", "AUTHENTICATION_SERVICE_ERROR");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseWrapper<String> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
        String requestUri = request.getDescription(false);
        // Don't log favicon requests as errors - they're normal browser behavior
        if (requestUri.contains("favicon.ico")) {
            log.debug("Favicon not found (normal browser behavior): {}", requestUri);
        } else {
            log.warn("Static resource not found: {}", requestUri);
        }
        return ResponseWrapper.error("Resource not found", "RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ListResponseWrapper<ResponseWrapper.ErrorDetails> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        logPotentialBaseControllerMiss("MethodArgumentNotValidException", ex, request);
        
        List<ResponseWrapper.ErrorDetails> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ResponseWrapper.ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .message(error.getField() + ": " + error.getDefaultMessage())
                        .details(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        String mainMessage = fieldErrors.isEmpty() ? "Validation error" : fieldErrors.get(0).getMessage();
        
        log.warn("Validation error: {}", mainMessage, ex);
        
        ResponseWrapper.ErrorDetails errorDetails = ResponseWrapper.ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .message(mainMessage)
                .details(fieldErrors)
                .build();
                
        ListResponseWrapper<ResponseWrapper.ErrorDetails> response = ListResponseWrapper.error("Validation failed", errorDetails);
        response.setData(fieldErrors);
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseWrapper<String> handleGenericException(Exception ex, WebRequest request) {
        log.error("🔴 UNHANDLED EXCEPTION in GlobalExceptionHandler - This should potentially be handled by BaseController: {} - Request: {}", 
                ex.getClass().getSimpleName(), request.getDescription(false), ex);
        return ResponseWrapper.error("Internal server error", "INTERNAL_ERROR");
    }

}
