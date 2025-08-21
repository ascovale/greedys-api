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
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> executeVoid(String operationName, String successMessage, VoidOperation operation) throws Exception {
        operation.execute();
        return ok(successMessage);
    }

    /**
     * Execute an operation with void return (for operations like delete, update status)
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected ResponseEntity<ResponseWrapper<String>> executeVoid(String operationName, VoidOperation operation) throws Exception {
        operation.execute();
        return ok("Operation " + operationName + " completed successfully");
    }

    /**
     * Execute an operation with standardized error handling
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> execute(String operation, OperationSupplier<T> supplier) throws Exception {
        T result = supplier.get();
        return ok(result, "Operation " + operation + " completed successfully");
    }

    /**
     * Execute an operation with custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> execute(String operation, String successMessage, OperationSupplier<T> supplier) throws Exception {
        T result = supplier.get();
        return ok(result, successMessage);
    }

    /**
     * Execute a CREATE operation with 201 Created response
     * Exceptions are handled by GlobalExceptionHandler
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> executeCreate(String operation, String successMessage, OperationSupplier<T> supplier) throws Exception {
        T result = supplier.get();
        return created(result, successMessage);
    }

    /**
     * Execute a CREATE operation with 201 Created response
     * Exceptions are handled by GlobalExceptionHandler
     */
    @CreateApiResponses
    protected <T> ResponseEntity<ResponseWrapper<T>> executeCreate(String operation, OperationSupplier<T> supplier) throws Exception {
        T result = supplier.get();
        return created(result, "Operation " + operation + " completed successfully");
    }

    /**
     * Execute a list operation with standardized error handling
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, OperationSupplier<List<T>> supplier) throws Exception {
        List<T> result = supplier.get();
        return okList(result, "Operation " + operation + " completed successfully");
    }

    /**
     * Execute a list operation with custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<List<T>>> executeList(String operation, String successMessage, OperationSupplier<List<T>> supplier) throws Exception {
        List<T> result = supplier.get();
        return okList(result, successMessage);
    }

    /**
     * Execute a paginated read operation with standardized error handling and pagination metadata
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> executePaginated(String operation, OperationSupplier<Page<T>> supplier) throws Exception {
        Page<T> page = supplier.get();
        return ResponseEntity.ok(ResponseWrapper.success(page, 
            String.format("Page %d of %d (%d total items)", 
                page.getNumber() + 1, page.getTotalPages(), page.getTotalElements())));
    }

    /**
     * Execute a paginated read operation with custom success message
     * Exceptions are handled by GlobalExceptionHandler
     */
    @ReadApiResponses
    protected <T> ResponseEntity<ResponseWrapper<Page<T>>> executePaginated(String operation, String successMessage, OperationSupplier<Page<T>> supplier) throws Exception {
        Page<T> page = supplier.get();
        return ResponseEntity.ok(ResponseWrapper.success(page, successMessage));
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
