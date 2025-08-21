
package com.application.admin.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
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
public class AdminServicesController extends BaseController {

    private final ServiceService serviceService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_READ')")
    @GetMapping("/types")
    @Operation(summary = "Get all service types", description = "Retrieve all service types.")
    public ResponseEntity<ResponseWrapper<List<ServiceTypeDto>>> getServiceTypes() {
        return executeList("get service types", () -> {
            Collection<ServiceTypeDto> serviceTypes = serviceService.getServiceTypes();
            return serviceTypes instanceof List ? (List<ServiceTypeDto>) serviceTypes : List.copyOf(serviceTypes);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
    @Operation(summary = "Create a new service type", description = "This method creates a new service type in the system.")
    @CreateApiResponses
    @PostMapping("/type/new")
    public ResponseEntity<ResponseWrapper<String>> newServiceType(@RequestBody String serviceTypeString) {
        return executeCreate("create service type", () -> {
            serviceService.newServiceType(serviceTypeString);
            return "Service type created successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
    @Operation(summary = "Update a service type", description = "This method updates an existing service type.")
    @WrapperType(dataClass = ServiceTypeDto.class, type = WrapperDataType.DTO)
    @PutMapping("/type/{typeId}/update")
    public ResponseEntity<ResponseWrapper<ServiceTypeDto>> updateServiceType(@PathVariable Long typeId, @RequestBody String serviceTypeString) {
        return execute("update service type", () -> {
            return serviceService.updateServiceTypeAndReturn(typeId, serviceTypeString);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
    @Operation(summary = "Delete a service type", description = "This method deletes a service type by its ID.")
    @DeleteMapping("/type/{typeId}/delete")
    public ResponseEntity<ResponseWrapper<String>> deleteServiceType(@PathVariable Long typeId) {
        return executeVoid("delete service type", "Service type deleted successfully", () -> {
            serviceService.deleteServiceType(typeId);
        });
    }
}
