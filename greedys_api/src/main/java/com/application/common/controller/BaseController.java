package com.application.common.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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
 * - Use the success response methods (ok, created, okPage, etc.) for successful operations
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
     */

    // ===================== LIST OPERATIONS ======================

    /**
     * Execute operation that returns a List<T>
     * 
     * @param operation Description of the operation for logging
     * @param supplier  Function that returns List<T>
     * @return ResponseEntity with wrapped List<T>
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, OperationSupplier<List<T>> supplier) {
        List<T> result = supplier.get();
        return ResponseEntity.ok(ResponseWrapper.successList(result));
    }

    // ===================== SLICE OPERATIONS ======================

    /**
     * Execute operation that returns a Slice<T>
     * 
     * @param operation Description of the operation for logging
     * @param supplier  Function that returns Slice<T>
     * @return ResponseEntity with wrapped Slice<T>
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Slice<T>>> executeSlice(String operation, OperationSupplier<Slice<T>> supplier) {
        Slice<T> result = supplier.get();
        return ResponseEntity.ok(ResponseWrapper.successSlice(result));
    }

    // ===================== PAGINATED OPERATIONS ======================

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

    // ===================== UTILITY METHODS ======================

    /**
     * Convert a List to a single-page Page for consistent API responses
     */
    protected <T> Page<T> toPage(List<T> list) {
        return new PageImpl<>(list, Pageable.unpaged(), list.size());
    }

    /**
     * Convert a List to a Pageable-aware Page for consistent API responses
     */
    protected <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<T> subList = list.subList(start, end);
        return new PageImpl<>(subList, pageable, list.size());
    }

    /**
     * Convert a List to a Slice for consistent API responses
     */
    protected <T> Slice<T> toSlice(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<T> subList = list.subList(start, end);
        boolean hasNext = end < list.size();
        return new SliceImpl<>(subList, pageable, hasNext);
    }
}
