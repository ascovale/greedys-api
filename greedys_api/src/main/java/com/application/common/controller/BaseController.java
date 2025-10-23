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

import lombok.extern.slf4j.Slf4j;

/**
 * Base controller with standardized response methods for direct entity responses.
 * 
 * This controller provides SUCCESS response methods and execution wrappers.
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
    protected <T> ResponseEntity<T> ok(T data) {
        return ResponseEntity.ok(data);
    }

    /**
     * Create a created response (201)
     */
    @CreateApiResponses
    protected <T> ResponseEntity<T> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(data);
    }

    /**
     * Create a no content response (204) for successful operations without data
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ===================== PAGE RESPONSES ======================

    /**
     * Create a successful page response with data
     */
    @ReadApiResponses
    protected <T> ResponseEntity<Page<T>> okPage(Page<T> page) {
        return ResponseEntity.ok(page);
    }

    // ===================== LIST RESPONSES ======================

    /**
     * Create a successful list response with data
     */
    @ReadApiResponses
    protected <T> ResponseEntity<List<T>> okList(List<T> list) {
        return ResponseEntity.ok(list);
    }

    // ===================== SLICE RESPONSES ======================

    /**
     * Create a successful slice response with data
     */
    @ReadApiResponses
    protected <T> ResponseEntity<Slice<T>> okSlice(Slice<T> slice) {
        return ResponseEntity.ok(slice);
    }

    // ===================== EXECUTION METHODS ======================

    /**
     * Execute an operation with void return (for operations like delete, update status)
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected ResponseEntity<Void> executeVoid(String operationName, VoidOperation operation) {
        operation.execute();
        return noContent();
    }

    /**
     * Execute an operation with void return and custom success message (for operations like delete, update status)
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected ResponseEntity<Void> executeVoid(String operationName, String successMessage, VoidOperation operation) {
        operation.execute();
        return noContent();
    }

    /**
     * Execute an operation with standardized error handling
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<T> execute(String operation, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return ok(result);
    }

    /**
     * Execute an operation with standardized error handling and custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<T> execute(String operation, String successMessage, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return ok(result);
    }

    /**
     * Execute a CREATE operation with 201 Created response
     * Exceptions are handled by GlobalExceptionHandler
     */
    @CreateApiResponses
    protected <T> ResponseEntity<T> executeCreate(String operation, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return created(result);
    }

    /**
     * Execute a CREATE operation with 201 Created response and custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @CreateApiResponses
    protected <T> ResponseEntity<T> executeCreate(String operation, String successMessage, OperationSupplier<T> supplier) {
        T result = supplier.get();
        return created(result);
    }

    /**
     * Execute operation that returns a List<T>
     * 
     * @param operation Description of the operation for logging
     * @param supplier  Function that returns List<T>
     * @return ResponseEntity with List<T>
     */
    @ReadApiResponses
    protected <T> ResponseEntity<List<T>> executeList(String operation, OperationSupplier<List<T>> supplier) {
        List<T> result = supplier.get();
        return okList(result);
    }

    /**
     * Execute operation that returns a Slice<T>
     * 
     * @param operation Description of the operation for logging
     * @param supplier  Function that returns Slice<T>
     * @return ResponseEntity with Slice<T>
     */
    @ReadApiResponses
    protected <T> ResponseEntity<Slice<T>> executeSlice(String operation, OperationSupplier<Slice<T>> supplier) {
        Slice<T> result = supplier.get();
        return okSlice(result);
    }

    /**
     * Execute a paginated read operation
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<Page<T>> executePaginated(String operation, OperationSupplier<Page<T>> supplier) {
        Page<T> page = supplier.get();
        return okPage(page);
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
