package com.application.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.service.AllergyService;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.customer.AllergyDTO;
import com.application.common.web.dto.customer.NewAllergyDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequestMapping("/admin/allergy")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Allergy", description = "Admin Allergy Management")
@RequiredArgsConstructor
public class AdminAllergyController extends BaseController {

    private final AllergyService allergyService;

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Create allergy", description = "Creates a new allergy for the specified user by their ID")
    @WrapperType(dataClass = AllergyDTO.class, responseCode = "201")
    @PostMapping("/new")
    public ResponseEntity<ResponseWrapper<AllergyDTO>> createAllergy(@RequestBody NewAllergyDTO allergyDto) {
        return executeCreate("create allergy", "Allergy created successfully", () -> {
            return allergyService.createAllergy(allergyDto);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy by its ID")
    @DeleteMapping("/{allergyId}/delete")
    public ResponseEntity<ResponseWrapper<String>> deleteAllergy(@PathVariable Long allergyId) {
        return executeVoid("delete allergy", "Allergy deleted successfully", () -> {
            allergyService.deleteAllergy(allergyId);
        });
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Modify allergy", description = "Modifies an existing allergy")
    @WrapperType(dataClass = AllergyDTO.class, type = WrapperDataType.DTO)
    @PutMapping("/{allergyId}/modify")
    public ResponseEntity<ResponseWrapper<AllergyDTO>> modifyAllergy(@PathVariable Long allergyId, @RequestBody NewAllergyDTO allergyDto) {
        return execute("modify allergy", () -> {
            return allergyService.modifyAllergyAndReturn(allergyId, allergyDto);
        });
    }
}
