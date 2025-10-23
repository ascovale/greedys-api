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
import com.application.admin.service.authentication.AdminCustomerAuthenticationService;
import com.application.common.controller.BaseController;
import com.application.common.web.dto.customer.CustomerDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.customer.persistence.model.Customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequestMapping("/admin/customer")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Customer", description = "Admin Customer Management")
@RequiredArgsConstructor
public class AdminCustomerController extends BaseController {
    private final AdminCustomerService adminCustomerService;
    private final AdminCustomerAuthenticationService adminCustomerAuthenticationService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Block customer", description = "Blocks a customer by their ID")
    @PutMapping("/{customerId}/block")
    public ResponseEntity<String> blockUser(@PathVariable Long customerId) {
        return execute("block customer", () -> {
            adminCustomerService.updateCustomerStatus(customerId, Customer.Status.BLOCKED);
            return "Customer blocked successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @PutMapping("/{customerId}/enable")
    public ResponseEntity<String> enableCustomer(@PathVariable Long customerId) {
        return execute("enable customer", () -> {
            adminCustomerService.updateCustomerStatus(customerId, Customer.Status.ENABLED);
            return "User enabled successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Enable customer by email", description = "Enables a customer by their email address")
    @PutMapping("/enable")
    public ResponseEntity<String> enableCustomerByEmail(@RequestParam String email) {
        return execute("enable customer by email", () -> {
            adminCustomerService.updateCustomerStatusByEmail(email, Customer.Status.ENABLED);
            return "Customer enabled successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "List customers with pagination", description = "Returns a paginated list of customers")
    @GetMapping("/customers/page")
    public ResponseEntity<Page<CustomerDTO>> listCustomersWithPagination(@RequestParam int page, @RequestParam int size) {
        return executePaginated("list customers", () -> adminCustomerService.findAll(PageRequest.of(page, size)));
        
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_SWITCH_TO_CUSTOMER')")
    @Operation(summary = "Get JWT Token of a customer", description = "Get JWT Token of a customer")
    @GetMapping("/login/{customerId}")
    public ResponseEntity<AuthResponseDTO> loginTokenHasCustomer(@PathVariable Long customerId) {
        return execute("get customer token", () -> {
            return adminCustomerAuthenticationService.adminLoginToCustomer(customerId);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Add role to customer", description = "Adds a role to a customer by their ID")
    @PutMapping("/{customerId}/add_role")
    public ResponseEntity<String> addRoleToCustomer(@PathVariable Long customerId, @RequestParam String roleName) {
        return execute("add role to customer", () -> {
            adminCustomerService.addRoleToCustomer(customerId, roleName);
            return "Role added successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Remove role from customer", description = "Removes a role from a customer by their ID")
    @PutMapping("/{customerId}/remove_role")
    public ResponseEntity<String> removeRoleFromCustomer(@PathVariable Long customerId, @RequestParam String roleName) {
        return execute("remove role from customer", () -> {
            adminCustomerService.removeRoleFromCustomer(customerId, roleName);
            return "Role removed successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Add privilege to role", description = "Aggiunge un permesso a un ruolo specifico")
    @PutMapping("/role/{roleName}/add_permission")
    public ResponseEntity<String> addPermissionToRole(@PathVariable String roleName, @RequestParam String permission) {
        return execute("add permission to role", () -> {
            adminCustomerService.addPrivilegeToRole(roleName, permission);
            return "Permission added successfully";
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Remove privilege from role", description = "Rimuove un permesso da un ruolo specifico")
    @PutMapping("/role/{roleName}/remove_permission")
    public ResponseEntity<String> removePermissionFromRole(@PathVariable String roleName, @RequestParam String permission) {
        return execute("remove permission from role", () -> {
            adminCustomerService.removePrivilegeFromRole(roleName, permission);
            return "Permission removed successfully";
        });
    }
}

