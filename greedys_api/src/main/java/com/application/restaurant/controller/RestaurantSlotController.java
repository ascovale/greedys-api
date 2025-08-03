package com.application.restaurant.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ResponseWrapper;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.SlotService;
import com.application.restaurant.web.dto.services.RestaurantNewSlotDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Slot Management", description = "Controller for managing slots")
@RestController
@RequestMapping("/restaurant/slot")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantSlotController extends BaseController {
    private final SlotService slotService;

    @Operation(summary = "Create a new slot", description = "This method creates a new slot.")
    @ApiResponse(responseCode = "201", description = "Slot created successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @CreateApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    @PostMapping("/new")
    public ResponseWrapper<String> newSlot(@RequestBody RestaurantNewSlotDTO slotDto) {
        return executeCreate("create new slot", "Slot created successfully", () -> {
            slotService.addSlot(RestaurantControllerUtils.getCurrentRUser().getId(), slotDto);
            return "success";
        });
    }

    @Operation(summary = "Cancel a slot", description = "This method cancels an existing slot.")
    @ApiResponse(responseCode = "200", description = "Slot canceled successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @ReadApiResponses
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    @DeleteMapping("/cancel/{slotId}")
    public ResponseWrapper<String> cancelSlot(@PathVariable Long slotId) {
        return execute("cancel slot", () -> {
            boolean isCanceled = slotService.cancelSlot(RestaurantControllerUtils.getCurrentRUser().getId(), slotId);
            if (isCanceled) {
                return "Slot canceled successfully";
            } else {
                throw new IllegalArgumentException("Slot not found");
            }
        });
    }
}
