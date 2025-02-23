package com.application.controller.restaurantUser;

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
@RequestMapping("/restaurant_user/{idRestaurantUser}/slot")
//@PreAuthorize("@securityService.isRestaurantUserPermission(#idRestaurantUser)")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantSlotController {

    @Autowired
    private SlotService slotService;
    @Operation(summary = "Create a new slot", description = "This method creates a new slot.", responses = {
            @ApiResponse(responseCode = "200", description = "Slot created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NewSlotDTO.class))),

    })
    @PreAuthorize("@securityService.hasRestaurantUserPrivilege(#idRestaurantUser, 'PRIVILEGE_GESTIONE_SERVIZI')")
    @PostMapping("/newSlot")
    public ResponseEntity<String> newSlot(@PathVariable Long idRestaurantUser, @RequestBody RestaurantNewSlotDTO slotDto) {
        slotService.addSlot(idRestaurantUser,slotDto);
        return ResponseEntity.ok().body("success");
    }

}
