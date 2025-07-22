package com.application.customer.controller;

import java.util.Date;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.get.CustomerStatisticsDTO;
import com.application.common.web.dto.put.UpdatePasswordDTO;
import com.application.common.web.error.InvalidOldPasswordException;
import com.application.common.web.util.GenericResponse;
import com.application.customer.model.Customer;
import com.application.customer.service.CustomerService;
import com.application.customer.service.authentication.CustomerAuthenticationService;

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
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
    private final CustomerService customerService;
    private final MessageSource messages;
    private final CustomerAuthenticationService customerAuthenticationService; // aggiunto
    //TODO: Implementare tutti i metodi per la configurazione delle notifiche del customer

    public CustomerController(CustomerService customerService,
            MessageSource messages,
            CustomerAuthenticationService customerAuthenticationService) { // aggiunto parametro
        this.customerService = customerService;
        this.messages = messages;
        this.customerAuthenticationService = customerAuthenticationService; // aggiunto
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
    @Operation(summary = "Update customer phone number", description = "Updates the phone number of a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "Phone number updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/update/phone")
    public GenericResponse updatePhone(@RequestParam String phone) {
        customerService.updatePhone(getCustomerId(), phone);
        return new GenericResponse("Phone number updated successfully");
    }

    @Operation(summary = "Update customer date of birth", description = "Updates the date of birth of a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "Date of birth updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/update/dateOfBirth")
    public GenericResponse updateDateOfBirth(@RequestParam Date dateOfBirth) {
        customerService.updateDateOfBirth(getCustomerId(), dateOfBirth);
        return new GenericResponse("Date of birth updated successfully");
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

    @Operation(summary = "Get current customer statistics", description = "Retrieves statistics for the current authenticated customer including no-show rate, reservations count, etc.")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerStatisticsDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/statistics/current")
    public CustomerStatisticsDTO getCurrentCustomerStatistics() {
        Customer currentCustomer = getCurrentCustomer();
        return customerService.getCustomerStatistics(currentCustomer.getId());
    }

    @Operation(summary = "Get customer statistics", description = "Retrieves statistics for a specific customer by ID including no-show rate, reservations count, etc.")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerStatisticsDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/{customerId}/statistics")
    public CustomerStatisticsDTO getCustomerStatistics(
            @Parameter(description = "The ID of the customer to retrieve statistics for", required = true, example = "1")
            @PathVariable Long customerId) {
        return customerService.getCustomerStatistics(customerId);
    }

    // TODO: This method should also be available for other user types

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
        if (!customerAuthenticationService.checkIfValidOldPassword(getCustomerId(), passwordDto.getOldPassword())) {
            throw new InvalidOldPasswordException();
        }
        customerAuthenticationService.changeCustomerPassword(getCustomerId(), passwordDto.getNewPassword());
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
