package com.application.controller.customer;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.service.AllergyService;
import com.application.service.CustomerService;
import com.application.web.dto.get.AllergyDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/customer/allergy")
@SecurityRequirement(name = "customerBearerAuth")
@Tag(name = "Allergy", description = "Controller per la gestione delle allergie del customer")

@RestController
public class AllergyController {
    private final CustomerService customerService;
    private final AllergyService allergyService;

    public AllergyController(CustomerService customerService,AllergyService allergyService) {
        this.allergyService = allergyService;
        this.customerService = customerService;
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Add allergy to customer", description = "Aggiunge un'allergia all'utente specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergia aggiunta con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @PostMapping("/add/{allergyId}")
    public GenericResponse addAllergyToUser(@PathVariable Long allergyId) {
        customerService.addAllergy(allergyId);
        return new GenericResponse("Allergy added successfully");
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Remove allergy from customer", description = "Rimuove un'allergia dall'utente specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergia rimossa con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @DeleteMapping("/remove/{allergyId}")
    public GenericResponse removeAllergyFromUser(@PathVariable Long idAllergy) {
        customerService.removeAllergy(idAllergy);
        return new GenericResponse("Allergy removed successfully");
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get allergies of customer", description = "Restituisce tutte le allergie dell'utente specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergie recuperate con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("/get")
    public List<AllergyDTO> getAllergiesOfCustomer() {
        return customerService.getAllergies(getCurrentCustomer().getId());
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get paginated allergies of customer", description = "Restituisce tutte le allergie dell'utente specificato tramite il suo ID in modo paginato")
    @ApiResponse(responseCode = "200", description = "Allergie recuperate con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "404", description = "Utente non trovato")
    @GetMapping("/get/paginated")
    public List<AllergyDTO> getPaginatedAllergiesOfCustomer(@RequestParam int page, @RequestParam int size) {
        return allergyService.getPaginatedAllergies( page, size);
    }

    @PreAuthorize("authentication.principal.isEnabled()")
    @Operation(summary = "Get allergy by ID", description = "Restituisce un'allergia specifica dell'utente tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergia recuperata con successo", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AllergyDTO.class)))
    @ApiResponse(responseCode = "404", description = "Allergia non trovata")
    @GetMapping("/get/{allergyId}")
    public AllergyDTO getAllergyById(@PathVariable Long allergyId) {
        return allergyService.getAllergyById(allergyId);
    }

    private Customer getCurrentCustomer() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    
}
