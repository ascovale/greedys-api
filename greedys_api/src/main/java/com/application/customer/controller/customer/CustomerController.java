package com.application.customer.controller.customer;

import java.util.Date;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.dto.customer.CustomerDTO;
import com.application.common.web.dto.customer.CustomerStatisticsDTO;
import com.application.common.web.dto.security.UpdatePasswordDTO;
import com.application.common.web.error.InvalidOldPasswordException;
import com.application.customer.persistence.model.Customer;
import com.application.customer.service.CustomerService;
import com.application.customer.service.authentication.CustomerAuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Profile Management", description = "Customer profile management operations")
@RestController
@RequestMapping("/customer")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class CustomerController extends BaseController {
    private final CustomerService customerService;
    private final MessageSource messages;
    private final CustomerAuthenticationService customerAuthenticationService;
    //TODO: Implementare tutti i metodi per la configurazione delle notifiche del customer

    @Operation(summary = "Get Customer ID", description = "Retrieves the ID of the current customer")
    @ReadApiResponses
    @GetMapping("/id")
    
    public ResponseEntity<Long> getCustomerId(@AuthenticationPrincipal Customer customer) {
        return execute("getCustomerId", () -> {
            return customer.getId();
        });
    }
    @Operation(summary = "Update customer phone number", description = "Updates the phone number of a specific customer by their ID")
    @ReadApiResponses
    @PutMapping("/update/phone")
    public ResponseEntity<CustomerDTO> updatePhone(@RequestParam String phone,@AuthenticationPrincipal Customer customer) {
        return execute("updatePhone", "Phone number updated successfully", () -> {
            return customerService.updatePhone(customer.getId(), phone);
        });
    }

    @Operation(summary = "Update customer date of birth", description = "Updates the date of birth of a specific customer by their ID")
    @ReadApiResponses
    @PutMapping("/update/dateOfBirth")
    public ResponseEntity<CustomerDTO> updateDateOfBirth(@RequestParam Date dateOfBirth,@AuthenticationPrincipal Customer customer) {
        return execute("updateDateOfBirth", "Date of birth updated successfully", () -> {
            return customerService.updateDateOfBirth(customer.getId(), dateOfBirth);
        });
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




    @Operation(summary = "Get current customer statistics", description = "Retrieves statistics for the current authenticated customer including no-show rate, reservations count, etc.")
    @GetMapping("/statistics/current")
    public ResponseEntity<CustomerStatisticsDTO> getCurrentCustomerStatistics(@AuthenticationPrincipal Customer customer) {
        return execute("getCurrentCustomerStatistics", () -> {
            return customerService.getCustomerStatistics(customer.getId());
        });
    }

    @Operation(summary = "Get customer statistics", description = "Retrieves statistics for a specific customer by ID including no-show rate, reservations count, etc.")
    @GetMapping("/{customerId}/statistics")
    public ResponseEntity<CustomerStatisticsDTO> getCustomerStatistics(
            @Parameter(description = "The ID of the customer to retrieve statistics for", required = true, example = "1")
            @PathVariable Long customerId) {
        return execute("getCustomerStatistics", () -> customerService.getCustomerStatistics(customerId));
    }

    // TODO: This method should also be available for other user types

    @Operation(summary = "Get current customer", description = "Retrieves the current authenticated customer")
    @GetMapping("/get")
    public ResponseEntity<CustomerDTO> getCustomer(@AuthenticationPrincipal Customer customer) {
        return execute("getCustomer", () -> new CustomerDTO(customer));
    }

    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @PostMapping(value = "/password/new_token")
    public ResponseEntity<String> changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "DTO containing the old and new password", required = true) @Valid UpdatePasswordDTO passwordDto,
            @AuthenticationPrincipal Customer customer) {
        return execute("changeUserPassword", () -> {
            if (!customerAuthenticationService.checkIfValidOldPassword(customer.getId(), passwordDto.getOldPassword())) {
                throw new InvalidOldPasswordException();
            }
            customerAuthenticationService.changeCustomerPassword(customer.getId(), passwordDto.getNewPassword());
            return messages.getMessage("message.updatePasswordSuc", null, locale);
        });
    }

    // TODO: Consider that when the customer is marked as deleted and then restored, it should be possible to confirm the token or regenerate it if the same email is used
    // Study carefully what to do

    @Operation(summary = "Delete customer", description = "Deletes a specific customer by their ID")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteCustomer(@AuthenticationPrincipal Customer customer) {
        return execute("deleteCustomer", "Customer deleted successfully", () -> {
            customerService.markCustomerHasDeleted(customer.getId());
            return "Customer deleted successfully";
        });
    }

    @Operation(summary = "Update customer first name", description = "Updates the first name of a specific customer by their ID")
    @PutMapping("/update/firstName")
    public ResponseEntity<CustomerDTO> updateFirstName(@RequestParam String firstName, @AuthenticationPrincipal Customer customer) {
        return execute("updateFirstName", "First name updated successfully", () -> {
            CustomerDTO updatedCustomer = customerService.updateFirstName(customer.getId(), firstName);
            return updatedCustomer;
        });
    }

    @Operation(summary = "Update customer last name", description = "Updates the last name of a specific customer by their ID")
    @PutMapping("/update/lastName")
    public ResponseEntity<CustomerDTO> updateLastName(@RequestParam String lastName, @AuthenticationPrincipal Customer customer) {
        return execute("updateLastName", "Last name updated successfully", () -> {
            CustomerDTO updatedCustomer = customerService.updateLastName(customer.getId(), lastName);
            return updatedCustomer;
        });
    }

    @Operation(summary = "Update customer email", description = "Updates the email of a specific customer by their ID")
    @PutMapping("/update/email")
    public ResponseEntity<CustomerDTO> updateEmail(@RequestParam String email, @AuthenticationPrincipal Customer customer) {
        return execute("updateEmail", "Email updated successfully", () -> {
            CustomerDTO updatedCustomer = customerService.updateEmail(customer.getId(), email);
            return updatedCustomer;
        });
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

