package com.application.common.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.application.common.controller.annotation.StandardApiResponses;
import com.application.common.web.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Base controller with standardized response methods
 */
@Slf4j
@StandardApiResponses  // ✅ Tutti i controller erediteranno automaticamente le risposte standard (400, 401, 403, 500)
public abstract class BaseController {

    /**
     * Create a successful response with data
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Create a successful response with data and custom message
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * Create a successful response for paginated data
     */
    protected <T> ResponseEntity<ApiResponse<Page<T>>> okPaginated(Page<T> page) {
        ApiResponse.ResponseMetadata metadata = createPaginationMetadata(page);
        String message = String.format("Page %d of %d (%d total items)", 
                page.getNumber() + 1, page.getTotalPages(), page.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(page, message, metadata));
    }

    /**
     * Create a successful response with only a message (no data)
     */
    protected ResponseEntity<ApiResponse<String>> ok(String message) {
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

    /**
     * Create a created response (201)
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message));
    }

    /**
     * Create a bad request response (400)
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "BAD_REQUEST"));
    }

    /**
     * Create a bad request response with custom error code
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message, String errorCode) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, errorCode));
    }

    /**
     * Create a not found response (404)
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message, "NOT_FOUND"));
    }

    /**
     * Create a conflict response (409)
     */
    protected <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message, "CONFLICT"));
    }

    /**
     * Create an internal server error response (500)
     */
    protected <T> ResponseEntity<ApiResponse<T>> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, "INTERNAL_SERVER_ERROR"));
    }

    /**
     * Create an unauthorized response (401)
     */
    protected <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message, "UNAUTHORIZED"));
    }

    /**
     * Create a forbidden response (403)
     */
    protected <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message, "FORBIDDEN"));
    }

    /**
     * Create a no content response (204) for successful operations without data
     */
    protected ResponseEntity<ApiResponse<String>> noContent(String message) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, message));
    }

    /**
     * Handle exceptions with standardized error response
     */
    protected <T> ResponseEntity<ApiResponse<T>> handleException(Exception e, String operation) {
        log.error("Error during {}: {}", operation, e.getMessage(), e);
        
        // Map specific exceptions to appropriate responses
        if (e instanceof IllegalArgumentException) {
            return badRequest(e.getMessage(), "INVALID_ARGUMENT");                           // 400
        } else if (e instanceof org.springframework.security.authentication.AuthenticationCredentialsNotFoundException) {
            return unauthorized("Authentication required: " + e.getMessage());              // 401
        } else if (e instanceof org.springframework.security.access.AccessDeniedException) {
            return forbidden("Access denied: " + e.getMessage());                          // 403
        } else if (e instanceof java.util.NoSuchElementException) {
            return notFound("Resource not found: " + e.getMessage());                      // 404
        } else if (e instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)                    // 405
                    .body(ApiResponse.error("Method not allowed: " + e.getMessage(), "METHOD_NOT_ALLOWED"));
        } else if (e instanceof org.springframework.dao.DataIntegrityViolationException) {
            return conflict("Data integrity violation: " + e.getMessage());               // 409
        } else if (e instanceof org.springframework.web.bind.MethodArgumentNotValidException) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)                 // 422
                    .body(ApiResponse.error("Validation failed: " + e.getMessage(), "VALIDATION_ERROR"));
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)                      // 408
                    .body(ApiResponse.error("Request timeout: " + e.getMessage(), "TIMEOUT"));
        } else if (e instanceof org.springframework.dao.OptimisticLockingFailureException) {
            return conflict("Optimistic locking failure: " + e.getMessage());             // 409
        } else if (e instanceof org.springframework.web.multipart.MaxUploadSizeExceededException) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)                    // 413
                    .body(ApiResponse.error("File too large: " + e.getMessage(), "FILE_TOO_LARGE"));
        } else if (e instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)               // 415
                    .body(ApiResponse.error("Unsupported media type: " + e.getMessage(), "UNSUPPORTED_MEDIA_TYPE"));
        } else if (e instanceof org.springframework.web.server.ServerErrorException) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)                          // 502
                    .body(ApiResponse.error("Bad gateway: " + e.getMessage(), "BAD_GATEWAY"));
        } else if (e instanceof java.net.ConnectException) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)                  // 503
                    .body(ApiResponse.error("Service unavailable: " + e.getMessage(), "SERVICE_UNAVAILABLE"));
        } else {
            return internalServerError("An unexpected error occurred during " + operation); // 500
        }
    }

    /**
     * Execute an operation with void return (for operations like delete, update status)
     */
    protected ResponseEntity<ApiResponse<String>> executeVoid(String operationName, String successMessage, VoidOperation operation) {
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
    protected ResponseEntity<ApiResponse<String>> executeVoid(String operationName, VoidOperation operation) {
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
    protected <T> ResponseEntity<ApiResponse<T>> execute(String operation, OperationSupplier<T> supplier) {
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
    protected <T> ResponseEntity<ApiResponse<T>> execute(String operation, String successMessage, OperationSupplier<T> supplier) {
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
    protected <T> ResponseEntity<ApiResponse<T>> executeCreate(String operation, String successMessage, OperationSupplier<T> supplier) {
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
    protected <T> ResponseEntity<ApiResponse<T>> executeCreate(String operation, OperationSupplier<T> supplier) {
        try {
            T result = supplier.get();
            return created(result, "Operation " + operation + " completed successfully");
        } catch (Exception e) {
            return handleException(e, operation);
        }
    }

    /**
     * Execute a paginated read operation with standardized error handling and pagination metadata
     */
    protected <T> ResponseEntity<ApiResponse<Page<T>>> executePaginated(String operation, OperationSupplier<Page<T>> supplier) {
        try {
            Page<T> page = supplier.get();
            return okPaginated(page);
        } catch (Exception e) {
            return handleException(e, operation);
        }
    }

    /**
     * Create pagination metadata from Spring Data Page
     */
    private <T> ApiResponse.ResponseMetadata createPaginationMetadata(Page<T> page) {
        return ApiResponse.ResponseMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
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
