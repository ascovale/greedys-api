package com.application.controller.pub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@SecurityRequirement(name = "bearerAuth")
public class RestaurantSlotController {

    @Autowired
    private SlotService slotService;

    @PreAuthorize("@securityService.hasRestaurantUserPermissionOnRestaurantWithId(#idRestaurant)")
    @Operation(summary = "Create a new slot", description = "This method creates a new slot.", responses = {
            @ApiResponse(responseCode = "200", description = "Slot created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewSlotDTO.class))),

    })
    @PostMapping("/newSlot/{idRestaurant}")
    public ResponseEntity<String> newSlot(@PathVariable Long idRestaurant, @RequestBody RestaurantNewSlotDTO slotDto) {
        slotService.addSlot(idRestaurant,slotDto);
        return ResponseEntity.ok().body("success");
    }

}
