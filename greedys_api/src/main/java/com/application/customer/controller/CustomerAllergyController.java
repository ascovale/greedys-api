package com.application.customer.controller;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.service.AllergyService;
import com.application.common.web.dto.get.AllergyDTO;
import com.application.common.web.util.GenericResponse;
import com.application.customer.model.Customer;
import com.application.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/customer/allergy")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Allergy", description = "Controller for managing customer allergies")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerAllergyController {
    // TODO: togliere ovunque isEnabled() che è ridondante che errore lancia spring
    // in caso di isEnabled false?
    // Rivedere il token che abilita il customer
    // Vedere se inserire il token di verifica mail ai vari utenti
    private final CustomerService customerService;
    private final AllergyService allergyService;

    @Operation(summary = "Add allergy to customer", description = "Adds an allergy to the currently authenticated customer using the allergy ID")
    @ApiResponse(responseCode = "200", description = "Allergy successfully added", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/add/{allergyId}")
    public GenericResponse addAllergyToCustomer(@PathVariable Long allergyId) {
        customerService.addAllergy(allergyId);
        return new GenericResponse("Allergy added successfully");
    }

    @Operation(summary = "Remove allergy from customer", description = "Removes an allergy from the currently authenticated customer using the allergy ID")
    @ApiResponse(responseCode = "200", description = "Allergy successfully removed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @DeleteMapping("/remove/{allergyId}")
    public GenericResponse removeAllergyFromUser(@PathVariable Long allergyId) {
        customerService.removeAllergy(allergyId);
        return new GenericResponse("Allergy removed successfully");
    }

    @Operation(summary = "Get allergies of customer", description = "Returns all allergies of the currently authenticated customer")
    @ApiResponse(responseCode = "200", description = "Allergies successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/allergies")
    public List<AllergyDTO> getAllergiesOfCustomer() {
        return customerService.getAllergies(getCurrentCustomer().getId());
    }
    //TODO: Da verificare tutti i metodi con pagine

    @Operation(summary = "Get paginated allergies of customer", description = "Returns paginated allergies of the currently authenticated customer")
    @ApiResponse(responseCode = "200", description = "Allergies successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/paginated")
    public List<AllergyDTO> getPaginatedAllergiesOfCustomer(@RequestParam int page, @RequestParam int size) {
        return customerService.getPaginatedAllergies(page, size);
    }

    @Operation(summary = "Get allergy by ID", description = "Returns a specific allergy of the currently authenticated customer using the allergy ID")
    @ApiResponse(responseCode = "200", description = "Allergy successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllergyDTO.class)))
    @ApiResponse(responseCode = "404", description = "Allergy not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/{allergyId}")
    public AllergyDTO getAllergyById(@PathVariable Long allergyId) {
        return allergyService.getAllergyById(allergyId);
    }

    private Customer getCurrentCustomer() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            log.warn("This should not happen");
            return null;
        }
    }

}
