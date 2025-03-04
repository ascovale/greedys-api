package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.AdminService;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User", description = "Admin management APIs for the Admin users")
public class AdminUsersController {
    // Riscrivere tutta la classe per implementare i metodi per dare togliere i permessi agli utenti admin
    // e per bloccare e sbloccare gli utenti admin o forse meglio chiamarlo supporto
    // aggiungere e rimuovere
    // aggiungere e rimuovere permessi
    private final AdminService adminService;

    @Autowired
    public AdminUsersController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Block user", description = "Blocks a user by their ID")
    @ApiResponse(responseCode = "200", description = "User blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/blockAdmin/{adminId}")
    public GenericResponse blockUser(@PathVariable Long adminId) {
        adminService.blockAdmin(adminId);
        return new GenericResponse("Admin blocked successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @ApiResponse(responseCode = "200", description = "User enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/enableAdmin/{adminId}")
    public GenericResponse enableUser(@PathVariable Long adminId) {
        adminService.enableUser(adminId);
        return new GenericResponse("User enabled successfully");
    }


}
