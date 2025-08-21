package com.application.common.controller.exception;

import java.net.ConnectException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.application.common.web.ResponseWrapper;
import com.application.common.web.error.RestaurantNotFoundException;
import com.application.common.web.error.UserNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // === ECCEZIONI BUSINESS LOGIC ===

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleRestaurantNotFoundException(
            RestaurantNotFoundException ex, WebRequest request) {
        log.warn("Restaurant not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error(ex.getMessage(), "RESTAURANT_NOT_FOUND"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error(ex.getMessage(), "USER_NOT_FOUND"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error(ex.getMessage(), "ENTITY_NOT_FOUND"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseWrapper<String>> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error("Resource not found: " + ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    // === ECCEZIONI DI VALIDAZIONE E RICHIESTE ===

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseWrapper<String>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Validation error: " + ex.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ResponseWrapper.<String>error("Validation failed: " + ex.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Missing parameter: " + ex.getParameterName(), "MISSING_PARAMETER"));
    }

    // === ECCEZIONI HTTP E FORMATO ===

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseWrapper.<String>error("Method not allowed: " + ex.getMessage(), "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseWrapper<String>> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request or invalid format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.<String>error("Unreadable request or invalid format", "INVALID_FORMAT"));
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ResponseWrapper<String>> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, WebRequest request) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseWrapper.<String>error("Unsupported media type: " + ex.getMessage(), "UNSUPPORTED_MEDIA_TYPE"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ResponseWrapper.<String>error("File too large: " + ex.getMessage(), "FILE_TOO_LARGE"));
    }

    // === ECCEZIONI DI DATI E INTEGRITÀ ===

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseWrapper<String>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.<String>error("Data integrity violation: " + ex.getMessage(), "DATA_INTEGRITY_VIOLATION"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ResponseWrapper<String>> handleOptimisticLockingFailure(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.<String>error("Optimistic locking failure: " + ex.getMessage(), "OPTIMISTIC_LOCKING_FAILURE"));
    }

    // === ECCEZIONI DI AUTENTICAZIONE E AUTORIZZAZIONE ===

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseWrapper<String>> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Invalid username or password", "BAD_CREDENTIALS"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ResponseWrapper<String>> handleDisabledException(DisabledException ex, WebRequest request) {
        log.error("Account disabled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Account is disabled", "ACCOUNT_DISABLED"));
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ResponseWrapper<String>> handleAccountExpiredException(AccountExpiredException ex, WebRequest request) {
        log.error("Account expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Account has expired", "ACCOUNT_EXPIRED"));
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ResponseWrapper<String>> handleCredentialsExpiredException(CredentialsExpiredException ex, WebRequest request) {
        log.error("Credentials expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Credentials have expired", "CREDENTIALS_EXPIRED"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleLockedException(LockedException ex, WebRequest request) {
        log.error("Account locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Account is locked", "ACCOUNT_LOCKED"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        log.error("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Invalid username or password", "AUTHENTICATION_FAILED"));
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ResponseWrapper<String>> handleInsufficientAuthentication(InsufficientAuthenticationException ex, WebRequest request) {
        log.warn("Insufficient authentication: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Insufficient authentication", "INSUFFICIENT_AUTHENTICATION"));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleAuthenticationCredentialsNotFound(AuthenticationCredentialsNotFoundException ex, WebRequest request) {
        log.warn("Authentication required: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Authentication required: " + ex.getMessage(), "AUTHENTICATION_REQUIRED"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseWrapper.<String>error("Access denied: " + ex.getMessage(), "ACCESS_DENIED"));
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ResponseWrapper<String>> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex, WebRequest request) {
        log.error("Authentication service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.<String>error("Authentication service error", "AUTHENTICATION_SERVICE_ERROR"));
    }

    // === ECCEZIONI DI RETE E TIMEOUT ===

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ResponseWrapper<String>> handleTimeoutException(TimeoutException ex, WebRequest request) {
        log.warn("Request timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ResponseWrapper.<String>error("Request timeout: " + ex.getMessage(), "TIMEOUT"));
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ResponseWrapper<String>> handleConnectException(ConnectException ex, WebRequest request) {
        log.error("Service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseWrapper.<String>error("Service unavailable: " + ex.getMessage(), "SERVICE_UNAVAILABLE"));
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<ResponseWrapper<String>> handleServerErrorException(ServerErrorException ex, WebRequest request) {
        log.error("Bad gateway: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ResponseWrapper.<String>error("Bad gateway: " + ex.getMessage(), "BAD_GATEWAY"));
    }

    // === ECCEZIONI DI RISORSE ===

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
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
            log.warn("Resource not found: {}", resourcePath);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.<String>error("Resource not found", "RESOURCE_NOT_FOUND"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<String>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.<String>error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }

}
