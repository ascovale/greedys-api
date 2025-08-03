package com.application.common.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.application.common.web.ListResponseWrapper;
import com.application.common.web.PageResponseWrapper;
import com.application.common.web.ResponseWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Base controller with standardized response methods
 */
@Slf4j
  // ✅ Tutti i controller erediteranno automaticamente le risposte standard (400, 401, 403, 500)
public abstract class BaseController {

    /**
     * Create a successful response with data
     */
    protected <T> ResponseWrapper<T> ok(T data) {
        return ResponseWrapper.success(data);
    }

    /**
     * Create a successful response with data and custom message
     */
    protected <T> ResponseWrapper<T> ok(T data, String message) {
        return ResponseWrapper.success(data, message);
    }

    /**
     * Create a successful response for paginated data
     */
    protected <T> PageResponseWrapper<T> okPaginated(Page<T> page) {
        String message = String.format("Page %d of %d (%d total items)", 
                page.getNumber() + 1, page.getTotalPages(), page.getTotalElements());
        
        return PageResponseWrapper.success(page, message);
    }

    /**
     * Create a successful response for list data
     */
    protected <T> ListResponseWrapper<T> okList(List<T> list) {
        String message = String.format("Retrieved %d items", list.size());
        
        return ListResponseWrapper.success(list, message);
    }

    /**
     * Create a successful response for list data with custom message
     */
    protected <T> ListResponseWrapper<T> okList(List<T> list, String message) {
        return ListResponseWrapper.success(list, message);
    }

    /**
     * Create a successful response with only a message (no data)
     */
    protected ResponseWrapper<String> ok(String message) {
        return ResponseWrapper.success(message, message);
    }

    /**
     * Create a created response (201)
     */
    @ResponseStatus(HttpStatus.CREATED)
    protected <T> ResponseWrapper<T> created(T data, String message) {
        return ResponseWrapper.created(data, message);
    }

    /**
     * Create a bad request response (400)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected <T> ResponseWrapper<T> badRequest(String message) {
        return ResponseWrapper.badRequest(message, "BAD_REQUEST");
    }

    /**
     * Create a bad request response with custom error code
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected <T> ResponseWrapper<T> badRequest(String message, String errorCode) {
        return ResponseWrapper.badRequest(message, errorCode);
    }

    /**
     * Create a not found response (404)
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected <T> ResponseWrapper<T> notFound(String message) {
        return ResponseWrapper.notFound(message);
    }

    /**
     * Create a conflict response (409)
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    protected <T> ResponseWrapper<T> conflict(String message) {
        return ResponseWrapper.conflict(message);
    }

    /**
     * Create an internal server error response (500)
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected <T> ResponseWrapper<T> internalServerError(String message) {
        return ResponseWrapper.internalServerError(message);
    }

    /**
     * Create an unauthorized response (401)
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    protected <T> ResponseWrapper<T> unauthorized(String message) {
        return ResponseWrapper.unauthorized(message);
    }

    /**
     * Create a forbidden response (403)
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    protected <T> ResponseWrapper<T> forbidden(String message) {
        return ResponseWrapper.forbidden(message);
    }

    /**
     * Create a no content response (204) for successful operations without data
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    protected ResponseWrapper<String> noContent(String message) {
        return ResponseWrapper.success(null, message);
    }

    /**
     * Handle exceptions with standardized error response
     */
    protected <T> ResponseWrapper<T> handleException(Exception e, String operation) {
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
            return ResponseWrapper.errorWithStatus(405, "Method not allowed: " + e.getMessage(), "METHOD_NOT_ALLOWED");                    // 405
        } else if (e instanceof org.springframework.dao.DataIntegrityViolationException) {
            return conflict("Data integrity violation: " + e.getMessage());               // 409
        } else if (e instanceof org.springframework.web.bind.MethodArgumentNotValidException) {
            return ResponseWrapper.errorWithStatus(422, "Validation failed: " + e.getMessage(), "VALIDATION_ERROR");                 // 422
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            return ResponseWrapper.errorWithStatus(408, "Request timeout: " + e.getMessage(), "TIMEOUT");                      // 408
        } else if (e instanceof org.springframework.dao.OptimisticLockingFailureException) {
            return conflict("Optimistic locking failure: " + e.getMessage());             // 409
        } else if (e instanceof org.springframework.web.multipart.MaxUploadSizeExceededException) {
            return ResponseWrapper.errorWithStatus(413, "File too large: " + e.getMessage(), "FILE_TOO_LARGE");                    // 413
        } else if (e instanceof org.springframework.web.server.UnsupportedMediaTypeStatusException) {
            return ResponseWrapper.errorWithStatus(415, "Unsupported media type: " + e.getMessage(), "UNSUPPORTED_MEDIA_TYPE");               // 415
        } else if (e instanceof org.springframework.web.server.ServerErrorException) {
            return ResponseWrapper.errorWithStatus(502, "Bad gateway: " + e.getMessage(), "BAD_GATEWAY");                          // 502
        } else if (e instanceof java.net.ConnectException) {
            return ResponseWrapper.errorWithStatus(503, "Service unavailable: " + e.getMessage(), "SERVICE_UNAVAILABLE");                  // 503
        } else {
            return internalServerError("An unexpected error occurred during " + operation); // 500
        }
    }

    /**
     * Execute an operation with void return (for operations like delete, update status)
     */
    protected ResponseWrapper<String> executeVoid(String operationName, String successMessage, VoidOperation operation) {
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
    protected ResponseWrapper<String> executeVoid(String operationName, VoidOperation operation) {
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
    protected <T> ResponseWrapper<T> execute(String operation, OperationSupplier<T> supplier) {
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
    protected <T> ResponseWrapper<T> execute(String operation, String successMessage, OperationSupplier<T> supplier) {
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
    protected <T> ResponseWrapper<T> executeCreate(String operation, String successMessage, OperationSupplier<T> supplier) {
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
    protected <T> ResponseWrapper<T> executeCreate(String operation, OperationSupplier<T> supplier) {
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
    protected <T> PageResponseWrapper<T> executePaginated(String operation, OperationSupplier<Page<T>> supplier) {
        try {
            Page<T> page = supplier.get();
            return okPaginated(page);
        } catch (Exception e) {
            return PageResponseWrapper.error("Error during " + operation + ": " + e.getMessage());
        }
    }

    /**
     * Execute a list read operation with standardized error handling
     */
    protected <T> ListResponseWrapper<T> executeList(String operation, OperationSupplier<List<T>> supplier) {
        try {
            List<T> list = supplier.get();
            return okList(list);
        } catch (Exception e) {
            return ListResponseWrapper.error("Error during " + operation + ": " + e.getMessage());
        }
    }

    /**
     * Execute a list read operation with custom success message
     */
    protected <T> ListResponseWrapper<T> executeList(String operation, String successMessage, OperationSupplier<List<T>> supplier) {
        try {
            List<T> list = supplier.get();
            return okList(list, successMessage);
        } catch (Exception e) {
            return ListResponseWrapper.error("Error during " + operation + ": " + e.getMessage());
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
