package com.application.controller.customer;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.persistence.model.customer.Customer;
import com.application.service.CustomerService;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.dto.put.UpdatePasswordDTO;
import com.application.web.error.InvalidOldPasswordException;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Customer", description = "Controller for managing customers")
@RestController
@RequestMapping("/customer")
@SecurityRequirement(name = "customerBearerAuth")
public class CustomerController {
    private final CustomerService customerService;
    private final MessageSource messages;

    //TODO: Emails should be sent in the service layer
    //TODO: Implementare tutti i metodi per la configurazione delle notifiche del customer

    public CustomerController(CustomerService customerService,
            MessageSource messages) {
        this.customerService = customerService;
        this.messages = messages;
    }

    @Operation(summary = "Get Customer ID", description = "Retrieves the ID of the current customer", responses = {
            @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/id")
    public Long getCustomerId() {
        return getCurrentCustomer().getId();
    }

    // TODO: Notification preferences settings
    // TODO: System preferences settings, e.g., how to receive notifications, but this is more restaurant-related

    /*
     * TODO: Implement this
     * 
     * @Operation(summary = "Get customer options", description =
     * "Retrieves the configured options for the current customer", responses = {
     * 
     * @ApiResponse(responseCode = "200", description = "Operation successful",
     * content = @Content(mediaType = "application/json", schema
     * = @Schema(implementation = Map.class))),
     * 
     * @ApiResponse(responseCode = "401", description = "Unauthorized"),
     * 
     * @ApiResponse(responseCode = "403", description = "Access denied")
     * })
     * 
     * @GetMapping("/options")
     * public Map<String, Object> getCustomerOptions() {
     * Customer currentUser = getCurrentCustomer();
     * if (currentUser == null) {
     * throw new IllegalStateException("Current customer not found");
     * }
     * return customerService.getCustomerOptions(currentUser.getId());
     * }
     */

    private Customer getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
            return (Customer) authentication.getPrincipal();
        }
        return null;
    }

    // TODO: This method should also be available for other user types
    // TODO: Implement customerStats to view characteristics like no-show, etc.
    // TODO: Implement restaurantStats

    @Operation(summary = "Get current customer", description = "Retrieves the current authenticated customer")
    @ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/get")
    public CustomerDTO getCustomer() {
        return new CustomerDTO(getCurrentCustomer());
    }

    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid old password or invalid data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping(value = "/password/new_token")
    public GenericResponse changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "DTO containing the old and new password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        if (!customerService.checkIfValidOldPassword(getCustomerId(), passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        customerService.changeCustomerPassword(getCustomerId(), passwordDto.getNewPassword());
        return new GenericResponse(messages.getMessage("message.updatePasswordSuc", null, locale));
    }

    // TODO: Consider that when the customer is marked as deleted and then restored, it should be possible to confirm the token or regenerate it if the same email is used
    // Study carefully what to do

    @Operation(summary = "Delete customer", description = "Deletes a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "Customer deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @DeleteMapping("/delete")
    public GenericResponse deleteCustomer() {
        customerService.markCustomerHasDeleted(getCustomerId());
        return new GenericResponse("Customer deleted successfully");
    }

    @Operation(summary = "Update customer first name", description = "Updates the first name of a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "First name updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/update/firstName")
    public GenericResponse updateFirstName(@RequestParam String firstName) {
        customerService.updateFirstName(getCustomerId(), firstName);
        return new GenericResponse("First name updated successfully");
    }

    @Operation(summary = "Update customer last name", description = "Updates the last name of a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "Last name updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/update/lastName")
    public GenericResponse updateLastName(@RequestParam String lastName) {
        customerService.updateLastName(getCustomerId(), lastName);
        return new GenericResponse("Last name updated successfully");
    }

    // TODO: Regenerate the email confirmation token
    // TODO: Request access credentials to ensure security

    @Operation(summary = "Update customer email", description = "Updates the email of a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "Email updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/update/email")
    public GenericResponse updateEmail(@RequestParam String email) {
        customerService.updateEmail(getCustomerId(), email);
        return new GenericResponse("Email updated successfully");
    }

    // TODO: Implement the method in the service to report abuse
    // The abuse should be reported to the system admin and should also contain a message
    /*
     * 
     * @Operation(summary = "Report restaurant abuse", description =
     * "Reports abuse by a restaurant")
     * 
     * @ApiResponse(responseCode = "200", description =
     * "Abuse reported successfully", content = @Content(mediaType =
     * "application/json", schema = @Schema(implementation =
     * GenericResponse.class)))
     * 
     * @ApiResponse(responseCode = "404", description =
     * "User or restaurant not found")
     * 
     * @PostMapping("/report/{restaurantId}")
     * public GenericResponse reportRestaurantAbuse(@PathVariable Long restaurantId)
     * {
     * customerService.reportRestaurantAbuse(restaurantId);
     * return new GenericResponse("Abuse reported successfully");
     * }
     */

}
