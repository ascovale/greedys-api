package com.application.controller.restaurantUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.service.SlotService;
import com.application.web.dto.post.NewSlotDTO;
import com.application.web.dto.post.restaurant.RestaurantNewSlotDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Slot", description = "Controller per la gestione degli slot")
@RestController
@RequestMapping("/restaurant/slot")
@SecurityRequirement(name = "restaurantBearerAuth")
public class RestaurantSlotController {
    //TODO: Cancella slot
    @Autowired
    private SlotService slotService;
    @Operation(summary = "Create a new slot", description = "This method creates a new slot.", responses = {
            @ApiResponse(responseCode = "200", description = "Slot created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewSlotDTO.class))),

    })
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    @PostMapping("/new_slot")
    public ResponseEntity<String> newSlot( @RequestBody RestaurantNewSlotDTO slotDto) {
        slotService.addSlot(ControllerUtils.getCurrentRestaurantUser().getId(),slotDto);
        return ResponseEntity.ok().body("success");
    }

    @Operation(summary = "Cancel a slot", description = "This method cancels an existing slot.", responses = {
        @ApiResponse(responseCode = "200", description = "Slot canceled", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Slot not found", content = @Content(mediaType = "application/json"))
    })
    @PreAuthorize("authentication.principal.isEnabled() & hasAuthority('PRIVILEGE_RESTAURANT_USER_SLOT_WRITE')")
    @PostMapping("/cancel_slot")
    public ResponseEntity<String> cancelSlot(@RequestBody Long slotId) {
        boolean isCanceled = slotService.cancelSlot(ControllerUtils.getCurrentRestaurantUser().getId(), slotId);
        if (isCanceled) {
        return ResponseEntity.ok().body("Slot canceled successfully");
        } else {
        return ResponseEntity.status(404).body("Slot not found");
        }
    }

}
