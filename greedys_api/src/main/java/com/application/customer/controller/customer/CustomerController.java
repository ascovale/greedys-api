package com.application.customer.controller.customer;

import java.util.Date;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
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
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.get.CustomerStatisticsDTO;
import com.application.common.web.dto.put.UpdatePasswordDTO;
import com.application.common.web.error.InvalidOldPasswordException;
import com.application.customer.controller.utils.CustomerControllerUtils;
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

@Tag(name = "Customer", description = "Controller for managing customers")
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
    @GetMapping("/id")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Long>> getCustomerId() {
        return execute("getCustomerId", () -> {
            return CustomerControllerUtils.getCurrentCustomerId();
        });
    }
    @Operation(summary = "Update customer phone number", description = "Updates the phone number of a specific customer by their ID")
    @PutMapping("/update/phone")
    public ResponseEntity<ApiResponse<String>> updatePhone(@RequestParam String phone) {
        return executeVoid("updatePhone", "Phone number updated successfully", () -> 
            customerService.updatePhone(CustomerControllerUtils.getCurrentCustomerId(), phone));
    }

    @Operation(summary = "Update customer date of birth", description = "Updates the date of birth of a specific customer by their ID")
    @PutMapping("/update/dateOfBirth")
    public ResponseEntity<ApiResponse<String>> updateDateOfBirth(@RequestParam Date dateOfBirth) {
        return executeVoid("updateDateOfBirth", "Date of birth updated successfully", () -> 
            customerService.updateDateOfBirth(CustomerControllerUtils.getCurrentCustomerId(), dateOfBirth));
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
    public ResponseEntity<ApiResponse<CustomerStatisticsDTO>> getCurrentCustomerStatistics() {
        return execute("getCurrentCustomerStatistics", () -> {
            Customer currentCustomer = CustomerControllerUtils.getCurrentCustomer();
            return customerService.getCustomerStatistics(currentCustomer.getId());
        });
    }

    @Operation(summary = "Get customer statistics", description = "Retrieves statistics for a specific customer by ID including no-show rate, reservations count, etc.")
    @GetMapping("/{customerId}/statistics")
    public ResponseEntity<ApiResponse<CustomerStatisticsDTO>> getCustomerStatistics(
            @Parameter(description = "The ID of the customer to retrieve statistics for", required = true, example = "1")
            @PathVariable Long customerId) {
        return execute("getCustomerStatistics", () -> customerService.getCustomerStatistics(customerId));
    }

    // TODO: This method should also be available for other user types

    @Operation(summary = "Get current customer", description = "Retrieves the current authenticated customer")
    @GetMapping("/get")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomer() {
        return execute("getCustomer", () -> new CustomerDTO(CustomerControllerUtils.getCurrentCustomer()));
    }

    @Operation(summary = "Generate new token for password change", description = "Changes the user's password after verifying the old password")
    @PostMapping(value = "/password/new_token")
    public ResponseEntity<ApiResponse<String>> changeUserPassword(
            @Parameter(description = "Locale for response messages") final Locale locale,
            @Parameter(description = "DTO containing the old and new password", required = true) @Valid UpdatePasswordDTO passwordDto) {
        return executeVoid("changeUserPassword", messages.getMessage("message.updatePasswordSuc", null, locale), () -> {
            if (!customerAuthenticationService.checkIfValidOldPassword(CustomerControllerUtils.getCurrentCustomerId(), passwordDto.getOldPassword())) {
                throw new InvalidOldPasswordException();
            }
            customerAuthenticationService.changeCustomerPassword(CustomerControllerUtils.getCurrentCustomerId(), passwordDto.getNewPassword());
        });
    }

    // TODO: Consider that when the customer is marked as deleted and then restored, it should be possible to confirm the token or regenerate it if the same email is used
    // Study carefully what to do

    @Operation(summary = "Delete customer", description = "Deletes a specific customer by their ID")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteCustomer() {
        return executeVoid("deleteCustomer", "Customer deleted successfully", () -> 
            customerService.markCustomerHasDeleted(CustomerControllerUtils.getCurrentCustomerId()));
    }

    @Operation(summary = "Update customer first name", description = "Updates the first name of a specific customer by their ID")
    @PutMapping("/update/firstName")
    public ResponseEntity<ApiResponse<String>> updateFirstName(@RequestParam String firstName) {
        return executeVoid("updateFirstName", "First name updated successfully", () -> 
            customerService.updateFirstName(CustomerControllerUtils.getCurrentCustomerId(), firstName));
    }

    @Operation(summary = "Update customer last name", description = "Updates the last name of a specific customer by their ID")
    @PutMapping("/update/lastName")
    public ResponseEntity<ApiResponse<String>> updateLastName(@RequestParam String lastName) {
        return executeVoid("updateLastName", "Last name updated successfully", () -> 
            customerService.updateLastName(CustomerControllerUtils.getCurrentCustomerId(), lastName));
    }

    @Operation(summary = "Update customer email", description = "Updates the email of a specific customer by their ID")
    @PutMapping("/update/email")
    public ResponseEntity<ApiResponse<String>> updateEmail(@RequestParam String email) {
        return executeVoid("updateEmail", "Email updated successfully", () -> 
            customerService.updateEmail(CustomerControllerUtils.getCurrentCustomerId(), email));
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
