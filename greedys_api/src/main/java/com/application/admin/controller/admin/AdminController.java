package com.application.admin.controller.admin;

import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.admin.controller.utils.AdminControllerUtils;
import com.application.admin.persistence.model.Admin;
import com.application.admin.service.AdminService;
import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.security.UpdatePasswordDTO;
import com.application.common.web.error.InvalidOldPasswordException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Admin Users Management")
@RequiredArgsConstructor
@Slf4j
public class AdminController extends BaseController {
    private final AdminService adminService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Block user", description = "Blocks a user by their ID")
    @PutMapping("/{adminId}/block")
    public ResponseWrapper<String> blockUser(@PathVariable Long adminId) {
        return executeVoid("block admin", "User blocked successfully", () -> {
            adminService.updateAdminStatus(adminId, Admin.Status.BLOCKED);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @PutMapping("/{adminId}/enable")
    public ResponseWrapper<String> enableUser(@PathVariable Long adminId) {
        return executeVoid("enable admin", "User enabled successfully", () -> {
            adminService.updateAdminStatus(adminId, Admin.Status.ENABLED);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_ADMIN_WRITE')")
    @Operation(summary = "Delete admin user", description = "Deletes an admin user by their ID")
    @PutMapping("/{adminId}/delete")
    public ResponseWrapper<String> deleteAdminUser(@PathVariable Long adminId) {
        return executeVoid("delete admin", "User deleted successfully", () -> {
            adminService.updateAdminStatus(adminId, Admin.Status.DELETED);
        });
    }

    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @PostMapping(value = "/password/new_token")
    public ResponseWrapper<String> changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "DTO containing the old and new password", required = true) @RequestBody @Valid UpdatePasswordDTO passwordDto) {
        return executeVoid("change admin password", "Password changed successfully", () -> {
            if (!adminService.checkIfValidOldPassword(getAdminId(), passwordDto.getOldPassword())) {
                throw new InvalidOldPasswordException();
            }
            adminService.changeAdminPassword(getAdminId(), passwordDto.getNewPassword());
        });
    }

    @Operation(summary = "Get Admin ID", description = "Retrieves the ID of the current admin")
    @GetMapping("/id")
    @ReadApiResponses
    public ResponseWrapper<Long> getAdminIdEndpoint() {
        return execute("get admin id", () -> AdminControllerUtils.getCurrentAdmin().getId());
    }
    
    private Long getAdminId() {
        return AdminControllerUtils.getCurrentAdmin().getId();
    }
}
