package com.application.customer.controller.customer;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.service.AllergyService;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.customer.AllergyDTO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
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
public class CustomerAllergyController extends BaseController {
    private final CustomerService customerService;
    private final AllergyService allergyService;

    @Operation(summary = "Add allergy to customer", description = "Adds an allergy to the currently authenticated customer using the allergy ID")
    @PostMapping("/add/{allergyId}")
    public ResponseEntity<ResponseWrapper<String>> addAllergyToCustomer(@PathVariable Long allergyId) {
        return executeCreate("addAllergyToCustomer", "Allergy added successfully", () -> {
            customerService.addAllergyToCustomer(allergyId);
            return "Allergy added successfully";
        });
    }

    @Operation(summary = "Remove allergy from customer", description = "Removes an allergy from the currently authenticated customer using the allergy ID")
    @DeleteMapping("/remove/{allergyId}")
    public ResponseEntity<ResponseWrapper<String>> removeAllergyFromUser(@PathVariable Long allergyId) {
        return executeVoid("removeAllergyFromUser", "Allergy removed successfully", () -> customerService.removeAllergyToCustomer(allergyId));
    }

    @Operation(summary = "Get allergies of customer", description = "Returns all allergies of the currently authenticated customer")
    @GetMapping("/allergies")
    
    public ResponseEntity<ResponseWrapper<List<AllergyDTO>>> getAllergiesOfCustomer(@AuthenticationPrincipal Customer customer) {
        return executeList("getAllergiesOfCustomer", () -> customerService.getAllergies(customer.getId()));
    }

    @Operation(summary = "Get paginated allergies of customer", description = "Returns paginated allergies of the currently authenticated customer")
    @GetMapping("/paginated")
    public ResponseEntity<ResponseWrapper<List<AllergyDTO>>> getPaginatedAllergiesOfCustomer(@RequestParam int page, @RequestParam int size) {
        return executePaginated("getPaginatedAllergiesOfCustomer", () -> customerService.getPaginatedAllergies(page, size));
    }

    @Operation(summary = "Get allergy by ID", description = "Returns a specific allergy of the currently authenticated customer using the allergy ID")
    @GetMapping("/{allergyId}")
    //PRVOa
    //jdifdj
    ///PPOEPEWOEWP
    public ResponseEntity<ResponseWrapper<AllergyDTO>> getAllergyById(@PathVariable Long allergyId) {
        return execute("getAllergyById", () -> allergyService.getAllergyById(allergyId));
    }

}
