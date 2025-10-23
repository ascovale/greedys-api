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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.application.common.web.ErrorDetails;
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
    public ResponseEntity<ErrorDetails> handleRestaurantNotFoundException(
            RestaurantNotFoundException ex, WebRequest request) {
        log.warn("Restaurant not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDetails.builder()
                        .code("RESTAURANT_NOT_FOUND")
                        .details(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDetails.builder()
                        .code("USER_NOT_FOUND")
                        .details(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDetails.builder()
                        .code("ENTITY_NOT_FOUND")
                        .details(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorDetails> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDetails.builder()
                        .code("RESOURCE_NOT_FOUND")
                        .details("Resource not found: " + ex.getMessage())
                        .build());
    }

    // === ECCEZIONI DI VALIDAZIONE E RICHIESTE ===

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDetails.builder()
                        .code("INVALID_ARGUMENT")
                        .details(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDetails> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .details("Validation error: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .details("Validation failed: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDetails> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDetails.builder()
                        .code("MISSING_PARAMETER")
                        .details("Missing parameter: " + ex.getParameterName())
                        .build());
    }

    // === ECCEZIONI HTTP E FORMATO ===

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDetails> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorDetails.builder()
                        .code("METHOD_NOT_ALLOWED")
                        .details("Method not allowed: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetails> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request or invalid format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDetails.builder()
                        .code("INVALID_FORMAT")
                        .details("Unreadable request or invalid format")
                        .build());
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ErrorDetails> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, WebRequest request) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorDetails.builder()
                        .code("UNSUPPORTED_MEDIA_TYPE")
                        .details("Unsupported media type: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDetails> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorDetails.builder()
                        .code("FILE_TOO_LARGE")
                        .details("File too large: " + ex.getMessage())
                        .build());
    }

    // === ECCEZIONI DI DATI E INTEGRITÀ ===

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDetails> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDetails.builder()
                        .code("DATA_INTEGRITY_VIOLATION")
                        .details("Data integrity violation: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDetails> handleOptimisticLockingFailure(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDetails.builder()
                        .code("OPTIMISTIC_LOCKING_FAILURE")
                        .details("Optimistic locking failure: " + ex.getMessage())
                        .build());
    }

    // === ECCEZIONI DI AUTENTICAZIONE E AUTORIZZAZIONE ===

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDetails> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("BAD_CREDENTIALS")
                        .details("Invalid username or password")
                        .build());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorDetails> handleDisabledException(DisabledException ex, WebRequest request) {
        log.error("Account disabled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("ACCOUNT_DISABLED")
                        .details("Account is disabled")
                        .build());
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ErrorDetails> handleAccountExpiredException(AccountExpiredException ex, WebRequest request) {
        log.error("Account expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("ACCOUNT_EXPIRED")
                        .details("Account has expired")
                        .build());
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ErrorDetails> handleCredentialsExpiredException(CredentialsExpiredException ex, WebRequest request) {
        log.error("Credentials expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("CREDENTIALS_EXPIRED")
                        .details("Credentials have expired")
                        .build());
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorDetails> handleLockedException(LockedException ex, WebRequest request) {
        log.error("Account locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("ACCOUNT_LOCKED")
                        .details("Account is locked")
                        .build());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        log.error("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("AUTHENTICATION_FAILED")
                        .details("Invalid username or password")
                        .build());
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorDetails> handleInsufficientAuthentication(InsufficientAuthenticationException ex, WebRequest request) {
        log.warn("Insufficient authentication: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("INSUFFICIENT_AUTHENTICATION")
                        .details("Insufficient authentication")
                        .build());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleAuthenticationCredentialsNotFound(AuthenticationCredentialsNotFoundException ex, WebRequest request) {
        log.warn("Authentication required: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("AUTHENTICATION_REQUIRED")
                        .details("Authentication required: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorDetails.builder()
                        .code("ACCESS_DENIED")
                        .details("Access denied: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ErrorDetails> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex, WebRequest request) {
        log.error("Authentication service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDetails.builder()
                        .code("AUTHENTICATION_SERVICE_ERROR")
                        .details("Authentication service error")
                        .build());
    }

    // === ECCEZIONI DI RETE E TIMEOUT ===

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex, WebRequest request) {
        // Client disconnesso durante la risposta - comportamento normale, non un errore
        log.debug("Client disconnected during response (normal behavior): {}", ex.getMessage());
        // Non restituire nessuna response dato che il client si è disconnesso
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorDetails> handleTimeoutException(TimeoutException ex, WebRequest request) {
        log.warn("Request timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ErrorDetails.builder()
                        .code("TIMEOUT")
                        .details("Request timeout: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorDetails> handleConnectException(ConnectException ex, WebRequest request) {
        log.error("Service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorDetails.builder()
                        .code("SERVICE_UNAVAILABLE")
                        .details("Service unavailable: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<ErrorDetails> handleServerErrorException(ServerErrorException ex, WebRequest request) {
        log.error("Bad gateway: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorDetails.builder()
                        .code("BAD_GATEWAY")
                        .details("Bad gateway: " + ex.getMessage())
                        .build());
    }

    // === ECCEZIONI DI RISORSE ===

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDetails> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
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
                .body(ErrorDetails.builder()
                        .code("RESOURCE_NOT_FOUND")
                        .details("Resource not found")
                        .build());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorDetails.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .details("An unexpected error occurred")
                        .build());
    }

}
