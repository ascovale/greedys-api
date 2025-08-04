
package com.application.admin.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.restaurant.ServiceTypeDto;
import com.application.restaurant.service.ServiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Services", description = "Admin Services Management")
@RestController
@RequestMapping("/admin/service")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AdminServicesC  {

    private final ServiceService serviceService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
    @GetMapping("/types222")
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    public ResponseEntity<ResponseWrapper<List<ServiceTypeDto>>> getServiceTypes() {
            Collection<ServiceTypeDto> serviceTypes = serviceService.getServiceTypes();
            return ResponseEntity.ok(ResponseWrapper.success((List<ServiceTypeDto>) serviceTypes));
    }
}
