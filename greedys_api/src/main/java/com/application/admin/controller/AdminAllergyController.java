package com.application.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.service.AllergyService;
import com.application.common.web.dto.post.NewAllergyDTO;
import com.application.common.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/allergy")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Allergy", description = "Admin Allergy Management")
@RequiredArgsConstructor
@Slf4j
public class AdminAllergyController {
    private final AllergyService allergyService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Create allergy", description = "Creates a new allergy for the specified user by their ID")
    @ApiResponse(responseCode = "200", description = "Allergy created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/new")
    public GenericResponse createAllergy(@RequestBody NewAllergyDTO allergyDto) {
        allergyService.createAllergy(allergyDto);
        return new GenericResponse("Allergy created successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy by its ID")
    @ApiResponse(responseCode = "200", description = "Allergy deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @DeleteMapping("/{allergyId}/delete")
    public GenericResponse deleteAllergy(@PathVariable Long allergyId) {
        allergyService.deleteAllergy(allergyId);
        return new GenericResponse("Allergy deleted successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Modify allergy", description = "Modifies an existing allergy")
    @ApiResponse(responseCode = "200", description = "Allergy modified successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/{idAllergy}/modify")
    public GenericResponse modifyAllergy(@PathVariable Long idAllergy, @RequestBody NewAllergyDTO allergyDto) {
        allergyService.modifyAllergy(idAllergy, allergyDto);
        return new GenericResponse("Allergy modified successfully");
    }
}
