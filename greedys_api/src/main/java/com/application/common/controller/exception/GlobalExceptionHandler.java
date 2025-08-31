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
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleRestaurantNotFoundException(
            RestaurantNotFoundException ex, WebRequest request) {
        log.warn("Restaurant not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(ex.getMessage(), "RESTAURANT_NOT_FOUND"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(ex.getMessage(), "USER_NOT_FOUND"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(ex.getMessage(), "ENTITY_NOT_FOUND"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleNoSuchElementException(NoSuchElementException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error("Resource not found: " + ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    // === ECCEZIONI DI VALIDAZIONE E RICHIESTE ===

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error("Validation error: " + ex.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Method argument validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ResponseWrapper.error("Validation failed: " + ex.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error("Missing parameter: " + ex.getParameterName(), "MISSING_PARAMETER"));
    }

    // === ECCEZIONI HTTP E FORMATO ===

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseWrapper.error("Method not allowed: " + ex.getMessage(), "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request or invalid format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error("Unreadable request or invalid format", "INVALID_FORMAT"));
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, WebRequest request) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseWrapper.error("Unsupported media type: " + ex.getMessage(), "UNSUPPORTED_MEDIA_TYPE"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, WebRequest request) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ResponseWrapper.error("File too large: " + ex.getMessage(), "FILE_TOO_LARGE"));
    }

    // === ECCEZIONI DI DATI E INTEGRITÀ ===

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.error("Data integrity violation: " + ex.getMessage(), "DATA_INTEGRITY_VIOLATION"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleOptimisticLockingFailure(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.error("Optimistic locking failure: " + ex.getMessage(), "OPTIMISTIC_LOCKING_FAILURE"));
    }

    // === ECCEZIONI DI AUTENTICAZIONE E AUTORIZZAZIONE ===

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Invalid username or password", "BAD_CREDENTIALS"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleDisabledException(DisabledException ex, WebRequest request) {
        log.error("Account disabled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Account is disabled", "ACCOUNT_DISABLED"));
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleAccountExpiredException(AccountExpiredException ex, WebRequest request) {
        log.error("Account expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Account has expired", "ACCOUNT_EXPIRED"));
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleCredentialsExpiredException(CredentialsExpiredException ex, WebRequest request) {
        log.error("Credentials expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Credentials have expired", "CREDENTIALS_EXPIRED"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleLockedException(LockedException ex, WebRequest request) {
        log.error("Account locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Account is locked", "ACCOUNT_LOCKED"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleUsernameNotFoundException(UsernameNotFoundException ex, WebRequest request) {
        log.error("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Invalid username or password", "AUTHENTICATION_FAILED"));
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleInsufficientAuthentication(InsufficientAuthenticationException ex, WebRequest request) {
        log.warn("Insufficient authentication: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Insufficient authentication", "INSUFFICIENT_AUTHENTICATION"));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleAuthenticationCredentialsNotFound(AuthenticationCredentialsNotFoundException ex, WebRequest request) {
        log.warn("Authentication required: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Authentication required: " + ex.getMessage(), "AUTHENTICATION_REQUIRED"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseWrapper.error("Access denied: " + ex.getMessage(), "ACCESS_DENIED"));
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleInternalAuthenticationServiceException(
            InternalAuthenticationServiceException ex, WebRequest request) {
        log.error("Authentication service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Authentication service error", "AUTHENTICATION_SERVICE_ERROR"));
    }

    // === ECCEZIONI DI RETE E TIMEOUT ===

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex, WebRequest request) {
        // Client disconnesso durante la risposta - comportamento normale, non un errore
        log.debug("Client disconnected during response (normal behavior): {}", ex.getMessage());
        // Non restituire nessuna response dato che il client si è disconnesso
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleTimeoutException(TimeoutException ex, WebRequest request) {
        log.warn("Request timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ResponseWrapper.error("Request timeout: " + ex.getMessage(), "TIMEOUT"));
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleConnectException(ConnectException ex, WebRequest request) {
        log.error("Service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseWrapper.error("Service unavailable: " + ex.getMessage(), "SERVICE_UNAVAILABLE"));
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleServerErrorException(ServerErrorException ex, WebRequest request) {
        log.error("Bad gateway: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ResponseWrapper.error("Bad gateway: " + ex.getMessage(), "BAD_GATEWAY"));
    }

    // === ECCEZIONI DI RISORSE ===

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
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
                .body(ResponseWrapper.error("Resource not found", "RESOURCE_NOT_FOUND"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<ErrorDetails>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }

}
