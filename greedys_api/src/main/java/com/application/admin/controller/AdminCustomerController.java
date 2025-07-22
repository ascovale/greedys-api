package com.application.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.service.AdminCustomerService;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.util.GenericResponse;
import com.application.customer.model.Customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/customer")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Customer", description = "Admin Customer Management")
@RequiredArgsConstructor
@Slf4j
public class AdminCustomerController {
    private final AdminCustomerService adminCustomerService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Block customer", description = "Blocks a customer by their ID")
    @ApiResponse(responseCode = "200", description = "User blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{customerId}/block")
    public GenericResponse blockUser(@PathVariable Long customerId) {
        adminCustomerService.updateCustomerStatus(customerId,Customer.Status.BLOCKED);
        return new GenericResponse("User blocked successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @ApiResponse(responseCode = "200", description = "User enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{customerId}/enable")
    public GenericResponse enableCustomer(@PathVariable Long customerId) {
        adminCustomerService.updateCustomerStatus(customerId,Customer.Status.ENABLED);
        return new GenericResponse("User enabled successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "List customers with pagination", description = "Returns a paginated list of customers")
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully", content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @GetMapping("/customers/page")
    public Page<CustomerDTO> listCustomersWithPagination(@RequestParam int page, @RequestParam int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return adminCustomerService.findAll(pageable);
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_SWITCH_TO_CUSTOMER')")
    @Operation(summary = "Get JWT Token of a customer", description = "Get JWT Token of a customer")
    @ApiResponse(responseCode = "200", description = "Token retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @GetMapping("/login/{customerId}")
    public ResponseEntity<?> loginTokenHasCustomer(@PathVariable Long customerId, HttpServletRequest request) {
        return ResponseEntity.ok(adminCustomerService.adminLoginToCustomer(customerId,request));
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Add role to customer", description = "Adds a role to a customer by their ID")
    @ApiResponse(responseCode = "200", description = "Role added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{customerId}/add_role")
    public GenericResponse addRoleToCustomer(@PathVariable Long customerId, @RequestParam String role) {
        adminCustomerService.addRoleToCustomer(customerId, role);
        return new GenericResponse("Role added successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Remove role from customer", description = "Removes a role from a customer by their ID")
    @ApiResponse(responseCode = "200", description = "Role removed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{customerId}/remove_role")
    public GenericResponse removeRoleFromCustomer(@PathVariable Long customerId, @RequestParam String role) {
        adminCustomerService.removeRoleFromCustomer(customerId, role);
        return new GenericResponse("Role removed successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Add privilege to role", description = "Aggiunge un permesso a un ruolo specifico")
    @ApiResponse(responseCode = "200", description = "Permesso aggiunto con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Richiesta non valida")
    @PutMapping("/role/{roleName}/add_permission")
    public ResponseEntity<GenericResponse> addPermissionToRole(@PathVariable String roleName, @RequestParam String permission) {
        adminCustomerService.addPrivilegeToRole(roleName, permission);
        return ResponseEntity.ok(new GenericResponse("Permission added successfully"));
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Remove privilege from role", description = "Rimuove un permesso da un ruolo specifico")
    @ApiResponse(responseCode = "200", description = "Permesso rimosso con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Richiesta non valida")
    @PutMapping("/role/{roleName}/remove_permission")
    public ResponseEntity<GenericResponse> removePermissionFromRole(@PathVariable String roleName, @RequestParam String permission) {
        adminCustomerService.removePrivilegeFromRole(roleName, permission);
        return ResponseEntity.ok(new GenericResponse("Permission removed successfully"));
    }

}
