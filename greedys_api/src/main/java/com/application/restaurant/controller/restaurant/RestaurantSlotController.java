package com.application.restaurant.controller.restaurant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.SlotService;
import com.application.restaurant.web.dto.services.RestaurantNewSlotDTO;

import io.swagger.v3.oas.annotations.Operation;
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
    
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    @PostMapping("/new")
    public ResponseEntity<SlotDTO> newSlot(@RequestBody RestaurantNewSlotDTO slotDto,
            @AuthenticationPrincipal RUser rUser) {
        return executeCreate("create new slot", "Slot created successfully", () -> {
            return slotService.addSlot(rUser.getId(), slotDto);
        });
    }

    @Operation(summary = "Cancel a slot", description = "This method cancels an existing slot.")
    
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    @DeleteMapping("/cancel/{slotId}")
    public ResponseEntity<String> cancelSlot(@PathVariable Long slotId,@AuthenticationPrincipal RUser rUser) {
        return execute("cancel slot", () -> {
            boolean isCanceled = slotService.cancelSlot(rUser.getId(), slotId);
            if (isCanceled) {
                return "Slot canceled successfully";
            } else {
                throw new IllegalArgumentException("Slot not found");
            }
        });
    }
}

