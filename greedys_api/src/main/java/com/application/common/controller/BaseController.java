package com.application.common.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.controller.annotation.StandardApiResponses;
import com.application.common.web.ResponseWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Base controller with standardized response methods using unified ResponseWrapper
 */
@Controller
@Slf4j
@StandardApiResponses
public class BaseController {

    // ===================== SINGLE OBJECT RESPONSES ======================

    /**
     * Create a successful response with data
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> ok(T data) {
        return ResponseEntity.ok(ResponseWrapper.success(data));
    }

    /**
     * Create a successful response with data and custom message
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> ok(T data, String message) {
        return ResponseEntity.ok(ResponseWrapper.success(data, message));
    }

    /**
     * Create a successful response with only a message (no data)
     */
    protected ResponseEntity<ResponseWrapper<String>> ok(String message) {
        return ResponseEntity.ok(ResponseWrapper.success(message, message));
    }

    /**
     * Create a created response (201)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(data, message));
    }

    // ===================== LIST RESPONSES ======================

    /**
     * Create a successful list response with data
     */
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> okList(List<T> data) {
        return ResponseEntity.ok(ResponseWrapper.successList(data));
    }

    /**
     * Create a successful list response with data and custom message
     */
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> okList(List<T> data, String message) {
        return ResponseEntity.ok(ResponseWrapper.successList(data, message));
    }

    /**
     * Create a created list response (201)
     */
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> createdList(List<T> data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.successList(data, message));
    }

    // ===================== PAGE RESPONSES ======================

    /**
     * Create a successful page response with data
     */
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> okPage(Page<T> page) {
        return ResponseEntity.ok(ResponseWrapper.successPage(page));
    }

    /**
     * Create a successful page response with data and custom message
     */
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> okPage(Page<T> page, String message) {
        return ResponseEntity.ok(ResponseWrapper.successPage(page, message));
    }

    // ===================== ERROR RESPONSES ======================

    /**
     * Create a bad request response (400)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ResponseWrapper.error(message, "BAD_REQUEST"));
    }

    /**
     * Create a bad request response with custom error code
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> badRequest(String message, String errorCode) {
        return ResponseEntity.badRequest()
                .body(ResponseWrapper.error(message, errorCode));
    }

    /**
     * Create a not found response (404)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(message, "NOT_FOUND"));
    }

    /**
     * Create a conflict response (409)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.error(message, "CONFLICT"));
    }

    /**
     * Create an internal server error response (500)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error(message, "INTERNAL_SERVER_ERROR"));
    }

    /**
     * Create an unauthorized response (401)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error(message, "UNAUTHORIZED"));
    }

    /**
     * Create a forbidden response (403)
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseWrapper.error(message, "FORBIDDEN"));
    }

    /**
     * Create a no content response (204) for successful operations without data
     */
    protected ResponseEntity<ResponseWrapper<String>> noContent(String message) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ResponseWrapper.success(null, message));
    }

    // ===================== EXECUTION METHODS ======================

    /**
     * Execute an operation with void return (for operations like delete, update status)
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> executeVoid(String operationName, String successMessage, VoidOperation operation) {
        try {
            operation.execute();
            return ok(successMessage);
        } catch (Exception e) {
            return handleException(e, operationName);
        }
    }

    /**
     * Execute an operation with void return (for operations like delete, update status)
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> executeVoid(String operationName, VoidOperation operation) {
        try {
            operation.execute();
            return ok("Operation " + operationName + " completed successfully");
        } catch (Exception e) {
            return handleException(e, operationName);
        }
    }

    /**
     * Execute an operation with standardized error handling
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> execute(String operation, OperationSupplier<T> supplier) {
        try {
            T result = supplier.get();
            return ok(result, "Operation " + operation + " completed successfully");
        } catch (Exception e) {
            return handleException(e, operation);
        }
    }

    /**
     * Execute an operation with custom success message
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> execute(String operation, String successMessage, OperationSupplier<T> supplier) {
        try {
            T result = supplier.get();
            return ok(result, successMessage);
        } catch (Exception e) {
            return handleException(e, operation);
        }
    }

    /**
     * Execute a CREATE operation with 201 Created response
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> executeCreate(String operation, String successMessage, OperationSupplier<T> supplier) {
        try {
            T result = supplier.get();
            return created(result, successMessage);
        } catch (Exception e) {
            return handleException(e, operation);
        }
    }

    /**
     * Execute a CREATE operation with 201 Created response
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> executeCreate(String operation, OperationSupplier<T> supplier) {
        try {
            T result = supplier.get();
            return created(result, "Operation " + operation + " completed successfully");
        } catch (Exception e) {
            return handleException(e, operation);
        }
    }

    /**
     * Execute a list operation with standardized error handling
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, OperationSupplier<List<T>> supplier) {
        try {
            List<T> result = supplier.get();
            return okList(result, "Operation " + operation + " completed successfully");
        } catch (Exception e) {
            return handleListException(e, operation);
        }
    }

    /**
     * Execute a list operation with custom success message
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, String successMessage, OperationSupplier<List<T>> supplier) {
        try {
            List<T> result = supplier.get();
            return okList(result, successMessage);
        } catch (Exception e) {
            return handleListException(e, operation);
        }
    }

    /**
     * Execute a paginated read operation with standardized error handling and pagination metadata
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> executePaginated(String operation, OperationSupplier<Page<T>> supplier) {
        try {
            Page<T> page = supplier.get();
            return ResponseEntity.ok(ResponseWrapper.success(page, 
                String.format("Page %d of %d (%d total items)", 
                    page.getNumber() + 1, page.getTotalPages(), page.getTotalElements())));
        } catch (Exception e) {
            return handlePageException(e, operation);
        }
    }

    /**
     * Execute a paginated read operation with custom success message
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> executePaginated(String operation, String successMessage, OperationSupplier<Page<T>> supplier) {
        try {
            Page<T> page = supplier.get();
            return ResponseEntity.ok(ResponseWrapper.success(page, successMessage));
        } catch (Exception e) {
            return handlePageException(e, operation);
        }
    }

    // ===================== EXCEPTION HANDLING ======================

    /**
     * Handle exceptions with standardized error response
     */
    protected <T> ResponseEntity<ResponseWrapper<T>> handleException(Exception e, String operation) {
        log.error("Error during {}: {}", operation, e.getMessage(), e);
        
        // Map specific exceptions to appropriate responses
        if (e instanceof IllegalArgumentException) {
            return badRequest(e.getMessage(), "INVALID_ARGUMENT");
        } else if (e instanceof BadCredentialsException) {
            return unauthorized("Invalid username or password");
        } else if (e instanceof DisabledException) {
            return unauthorized("Account is disabled");
        } else if (e instanceof AccountExpiredException) {
            return unauthorized("Account has expired");
        } else if (e instanceof CredentialsExpiredException) {
            return unauthorized("Credentials have expired");
        } else if (e instanceof LockedException) {
            return unauthorized("Account is locked");
        } else if (e instanceof UsernameNotFoundException) {
            return unauthorized("Invalid username or password");
        } else if (e instanceof jakarta.persistence.EntityNotFoundException) {
            return notFound(e.getMessage());
        } else if (e instanceof InsufficientAuthenticationException) {
            return unauthorized("Insufficient authentication");
        } else if (e instanceof AuthenticationCredentialsNotFoundException) {
            return unauthorized("Authentication required: " + e.getMessage());
        } else if (e instanceof org.springframework.security.access.AccessDeniedException) {
            return forbidden("Access denied: " + e.getMessage());
        } else if (e instanceof java.util.NoSuchElementException) {
            return notFound("Resource not found: " + e.getMessage());
        } else if (e instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(ResponseWrapper.error("Method not allowed: " + e.getMessage(), "METHOD_NOT_ALLOWED"));
        } else if (e instanceof org.springframework.dao.DataIntegrityViolationException) {
            return conflict("Data integrity violation: " + e.getMessage());
        } else if (e instanceof org.springframework.web.bind.MethodArgumentNotValidException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ResponseWrapper.error("Validation failed: " + e.getMessage(), "VALIDATION_ERROR"));
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(ResponseWrapper.error("Request timeout: " + e.getMessage(), "TIMEOUT"));
        } else if (e instanceof org.springframework.dao.OptimisticLockingFailureException) {
            return conflict("Optimistic locking failure: " + e.getMessage());
        } else if (e instanceof org.springframework.web.multipart.MaxUploadSizeExceededException) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ResponseWrapper.error("File too large: " + e.getMessage(), "FILE_TOO_LARGE"));
        } else if (e instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ResponseWrapper.error("Unsupported media type: " + e.getMessage(), "UNSUPPORTED_MEDIA_TYPE"));
        } else if (e instanceof org.springframework.web.server.ServerErrorException) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ResponseWrapper.error("Bad gateway: " + e.getMessage(), "BAD_GATEWAY"));
        } else if (e instanceof java.net.ConnectException) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ResponseWrapper.error("Service unavailable: " + e.getMessage(), "SERVICE_UNAVAILABLE"));
        } else {
            return internalServerError("An unexpected error occurred during " + operation);
        }
    }

    /**
     * Handle exceptions for List responses with standardized error response
     */
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> handleListException(Exception e, String operation) {
        log.error("Error during {}: {}", operation, e.getMessage(), e);
        
        // Map specific exceptions to appropriate responses
        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Invalid argument: " + e.getMessage()));
        } else if (e instanceof jakarta.persistence.EntityNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseWrapper.error(e.getMessage()));
        } else if (e instanceof org.springframework.security.access.AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseWrapper.error("Access denied: " + e.getMessage()));
        } else if (e instanceof java.util.NoSuchElementException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseWrapper.error("Resource not found: " + e.getMessage()));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("An unexpected error occurred during " + operation));
        }
    }

    /**
     * Handle exceptions for Page responses with standardized error response
     */
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> handlePageException(Exception e, String operation) {
        log.error("Error during {}: {}", operation, e.getMessage(), e);
        
        // Map specific exceptions to appropriate responses
        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Invalid argument: " + e.getMessage()));
        } else if (e instanceof jakarta.persistence.EntityNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseWrapper.error(e.getMessage()));
        } else if (e instanceof org.springframework.security.access.AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseWrapper.error("Access denied: " + e.getMessage()));
        } else if (e instanceof java.util.NoSuchElementException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseWrapper.error("Resource not found: " + e.getMessage()));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseWrapper.error("An unexpected error occurred during " + operation));
        }
    }

    /**
     * Functional interface for operations that can throw exceptions
     */
    @FunctionalInterface
    protected interface OperationSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Functional interface for void operations that can throw exceptions
     */
    @FunctionalInterface
    protected interface VoidOperation {
        void execute() throws Exception;
    }
}
