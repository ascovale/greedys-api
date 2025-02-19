package com.application.controller.customer;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.CustomerService;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;

@RequestMapping("/customer/allergy")
@RestController
public class AllergyController {
    private final CustomerService userService;

    public AllergyController(CustomerService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Add allergy to customer", description = "Aggiunge un'allergia all'utente specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergia aggiunta con successo", 
                 content = @Content(mediaType = "application/json", 
                                    schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @PostMapping("/newAllergy")
    public GenericResponse addAllergyToUser(@RequestParam Long idAllergy) {
                userService.addAllergy(idAllergy);
        return new GenericResponse("Allergy added successfully");
    }

    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Remove allergy from customer", description = "Rimuove un'allergia dall'utente specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergia rimossa con successo", 
                 content = @Content(mediaType = "application/json", 
                                    schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @DeleteMapping("/deleteAllergy")
    public GenericResponse removeAllergyFromUser(@PathVariable Long idAllergy) {
        userService.removeAllergy(idAllergy);
        return new GenericResponse("Allergy removed successfully");
    }
}
