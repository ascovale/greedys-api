package com.application.common.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.controller.annotation.StandardApiResponses;
import com.application.common.web.ResponseWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Base controller with standardized response methods using unified ResponseWrapper.
 * 
 * This controller provides only SUCCESS response methods and execution wrappers.
 * ERROR handling is delegated to GlobalExceptionHandler through thrown exceptions.
 * 
 * Controllers should:
 * - Use the success response methods (ok, created, okList, etc.) for successful operations
 * - Use the execute methods to wrap business logic that may throw exceptions
 * - Let exceptions bubble up to GlobalExceptionHandler instead of handling them locally
 */
@Controller
@Slf4j
@StandardApiResponses
public class BaseController {

    // ===================== SINGLE OBJECT RESPONSES ======================

    /**
     * Create a successful response with data
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> ok(T data) {
        return ResponseEntity.ok(ResponseWrapper.success(data));
    }

    /**
     * Create a successful response with data and custom message
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> ok(T data, String message) {
        return ResponseEntity.ok(ResponseWrapper.success(data, message));
    }

    /**
     * Create a successful response with only a message (no data)
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> ok(String message) {
        return ResponseEntity.ok(ResponseWrapper.success(message, message));
    }

    /**
     * Create a created response (201)
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success(data, message));
    }

    // ===================== LIST RESPONSES ======================

    /**
     * Create a successful list response with data
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> okList(List<T> data) {
        return ResponseEntity.ok(ResponseWrapper.successList(data));
    }

    /**
     * Create a successful list response with data and custom message
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> okList(List<T> data, String message) {
        return ResponseEntity.ok(ResponseWrapper.successList(data, message));
    }

    /**
     * Create a created list response (201)
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> createdList(List<T> data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.successList(data, message));
    }

    // ===================== PAGE RESPONSES ======================

    /**
     * Create a successful page response with data
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> okPage(Page<T> page) {
        return ResponseEntity.ok(ResponseWrapper.successPage(page));
    }

    /**
     * Create a successful page response with data and custom message
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> okPage(Page<T> page, String message) {
        return ResponseEntity.ok(ResponseWrapper.successPage(page, message));
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
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> executeVoid(String operationName, String successMessage, VoidOperation operation) {
        operation.execute();
        return ok(successMessage);
    }

    /**
     * Execute an operation with void return (for operations like delete, update status)
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> executeVoid(String operationName, VoidOperation operation) {
        operation.execute();
        return ok("Operation " + operationName + " completed successfully");
    }

    /**
     * Execute an operation with standardized error handling
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> execute(String operation, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return ok(result, "Operation " + operation + " completed successfully");
    }

    /**
     * Execute an operation with custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> execute(String operation, String successMessage, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return ok(result, successMessage);
    }

    /**
     * Execute a CREATE operation with 201 Created response
     * Exceptions are handled by GlobalExceptionHandler
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> executeCreate(String operation, String successMessage, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return created(result, successMessage);
    }

    /**
     * Execute a CREATE operation with 201 Created response
     * Exceptions are handled by GlobalExceptionHandler
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> executeCreate(String operation, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return created(result, "Operation " + operation + " completed successfully");
    }

    /**
     * Execute a list operation with standardized error handling
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, OperationSupplier<List<T>> supplier) {
        List<T> result = supplier.get();
        return okList(result, "Operation " + operation + " completed successfully");
    }

    /**
     * Execute a list operation with custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, String successMessage, OperationSupplier<List<T>> supplier) {
        List<T> result = supplier.get();
        return okList(result, successMessage);
    }

        /**
     * Execute a paginated read operation with automatic message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> executePaginated(String operation, OperationSupplier<Page<T>> supplier) {
        Page<T> page = supplier.get();
        return ResponseEntity.ok(ResponseWrapper.successPage(page, 
            String.format("Page %d of %d (%d total items)", 
                page.getNumber() + 1, page.getTotalPages(), page.getTotalElements())));
    }

    /**
     * Execute a paginated read operation with custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> executePaginated(String operation, String successMessage, OperationSupplier<Page<T>> supplier) {
        Page<T> page = supplier.get();
        return ResponseEntity.ok(ResponseWrapper.successPage(page, successMessage));
    }

    /**
     * Functional interface for operations that can throw exceptions
     */
    @FunctionalInterface
    protected interface OperationSupplier<T> {
        T get();
    }

    /**
     * Functional interface for void operations that can throw exceptions
     */
    @FunctionalInterface
    protected interface VoidOperation {
        void execute();
    }
}
