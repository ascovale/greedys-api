package com.application.common.spring.swagger.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.spring.swagger.util.ResponseWrapperCatalogService;
import com.application.common.web.ResponseWrapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Controller che espone il catalogo dei ResponseWrapper generato a startup.
 * Estende `BaseController` e usa `execute(...)` per uniformare le risposte.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class ResponseWrapperCatalogController extends BaseController {

    private final ResponseWrapperCatalogService catalogService;

    @GetMapping(path = "/response-wrapper-catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseWrapper<Object>> getCatalog() {
        return execute("get response wrapper catalog", () -> {
            Object catalog = catalogService.getCatalog();
            if (catalog == null) {
                throw new EntityNotFoundException("Response wrapper catalog not found");
            }
            return catalog;
        });
    }

}
