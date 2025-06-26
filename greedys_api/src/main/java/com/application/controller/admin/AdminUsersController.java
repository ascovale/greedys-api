package com.application.controller.admin;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.admin.Admin;
import com.application.service.AdminService;
import com.application.web.dto.put.UpdatePasswordDTO;
import com.application.web.error.InvalidOldPasswordException;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/admin")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Admin Users Management")
public class AdminUsersController {
    // TODO: aggiungere ruoli e permessi ai ruoli come metodi
    // Riscrivere tutta la classe per implementare i metodi per dare togliere i
    // permessi agli utenti admin
    // e per bloccare e sbloccare gli utenti admin o forse meglio chiamarlo supporto
    // aggiungere e rimuovere
    // aggiungere e rimuovere permessi
    private final AdminService adminService;
    private final MessageSource messages;

    @Autowired
    public AdminUsersController(AdminService adminService, MessageSource messages) {
        this.messages = messages;
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Block user", description = "Blocks a user by their ID")
    @ApiResponse(responseCode = "200", description = "User blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{adminId}/block")
    public GenericResponse blockUser(@PathVariable Long adminId) {
        adminService.updateAdminStatus(adminId, Admin.Status.BLOCKED);
        return new GenericResponse("Admin blocked successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @ApiResponse(responseCode = "200", description = "User enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{adminId}/enable")
    public GenericResponse enableUser(@PathVariable Long adminId) {
        adminService.updateAdminStatus(adminId, Admin.Status.ENABLED);
        return new GenericResponse("User enabled successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Delete admin user", description = "Deletes an admin user by their ID")
    @ApiResponse(responseCode = "200", description = "Admin user deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{adminId}/delete")
    public GenericResponse deleteAdminUser(@PathVariable Long adminId) {
        adminService.updateAdminStatus(adminId, Admin.Status.DELETED);
        return new GenericResponse("Admin user deleted successfully");
    }

    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid old password or invalid data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping(value = "/password/new_token")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "DTO containing the old and new password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        if (!adminService.checkIfValidOldPassword(getAdminId(), passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        adminService.changeAdminPassword(getAdminId(), passwordDto.getNewPassword());
        return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
    }

    @Operation(summary = "Get Admin ID", description = "Retrieves the ID of the current admin", responses = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/id")
    public Long getAdminId() {
        return getCurrentAdmin().getId();
    }

    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Admin) {
            return (Admin) authentication.getPrincipal();
        }
        return null;
    }
}
