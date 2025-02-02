package com.application.controller.restaurant;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import com.application.service.SlotService;
import com.application.persistence.model.reservation.Slot;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.NewSlotDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@Tag(name = "Slot", description = "Controller per la gestione degli slot")
@RestController
@RequestMapping("/slot")
@SecurityRequirement(name = "bearerAuth")
public class SlotController {
    
    @Autowired
    private SlotService slotService;

    @Operation(summary = "Get all slots")
    @GetMapping
    public Collection<Slot> getAllSlots (@RequestParam String param) {
        return slotService.findAll();
    }

    @Operation(summary = "Get slot by id", description = "This method returns a slot by its id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Slot found",
                                    content = @Content(mediaType = "application/json", 
                                    schema = @Schema(implementation = SlotDTO.class))),
                    
            })
    @GetMapping("/{id}")
    public SlotDTO getSlotById(@PathVariable Long id) {
        return slotService.findById(id);
    }
    
    
    
}
