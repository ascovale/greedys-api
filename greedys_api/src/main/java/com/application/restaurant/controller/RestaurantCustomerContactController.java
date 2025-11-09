package com.application.restaurant.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RestaurantCustomerContactService;
import com.application.restaurant.web.dto.customer.RestaurantCustomerContactDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing restaurant customer contacts/address book.
 * Provides endpoints for viewing and searching customer information
 * for restaurant staff.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/restaurant/contacts")
@Tag(name = "Restaurant Customer Contacts", description = "Manage restaurant customer address book")
@RequiredArgsConstructor
@Slf4j
public class RestaurantCustomerContactController extends BaseController {

    private final RestaurantCustomerContactService contactService;

    @Operation(
        summary = "Get all customer contacts", 
        description = "Retrieve all customers who have made reservations at the restaurant"
    )
    @ReadApiResponses
    @GetMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CUSTOMER_READ')")
    public ResponseEntity<List<RestaurantCustomerContactDTO>> getAllContacts(
            @AuthenticationPrincipal RUser rUser) {
        return executeList("get all customer contacts", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return contactService.getRestaurantContacts(restaurantId);
        });
    }

    @Operation(
        summary = "Get customer contacts with pagination", 
        description = "Retrieve customer contacts with pagination and sorting"
    )
    @ReadApiResponses
    @GetMapping("/pageable")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CUSTOMER_READ')")
    public ResponseEntity<Page<RestaurantCustomerContactDTO>> getContactsPageable(
            @AuthenticationPrincipal RUser rUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        return executePaginated("get customer contacts pageable", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return contactService.getRestaurantContactsPageable(restaurantId, page, size, sortBy);
        });
    }

    @Operation(
        summary = "Search customer contacts", 
        description = "Search customers by name, email, or phone number"
    )
    @ReadApiResponses
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CUSTOMER_READ')")
    public ResponseEntity<List<RestaurantCustomerContactDTO>> searchContacts(
            @AuthenticationPrincipal RUser rUser,
            @Parameter(description = "Search term for name, email, or phone", required = true)
            @RequestParam String q) {
        return executeList("search customer contacts", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return contactService.searchRestaurantContacts(restaurantId, q);
        });
    }

    @Operation(
        summary = "Get unregistered contacts only", 
        description = "Retrieve only customers who haven't created online accounts"
    )
    @ReadApiResponses
    @GetMapping("/unregistered")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CUSTOMER_READ')")
    public ResponseEntity<List<RestaurantCustomerContactDTO>> getUnregisteredContacts(
            @AuthenticationPrincipal RUser rUser) {
        return executeList("get unregistered customer contacts", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return contactService.getUnregisteredContacts(restaurantId);
        });
    }

    @Operation(
        summary = "Get customer contact details", 
        description = "Retrieve detailed information for a specific customer"
    )
    @ReadApiResponses
    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CUSTOMER_READ')")
    public ResponseEntity<RestaurantCustomerContactDTO> getContactDetails(
            @AuthenticationPrincipal RUser rUser,
            @Parameter(description = "Customer ID", required = true)
            @PathVariable Long customerId) {
        return execute("get customer contact details", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return contactService.getCustomerContactDetails(restaurantId, customerId);
        });
    }

    @Operation(
        summary = "Get contact statistics", 
        description = "Retrieve statistics about customer contacts (total, registered, unregistered)"
    )
    @ReadApiResponses
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_CUSTOMER_READ')")
    public ResponseEntity<RestaurantCustomerContactService.ContactStatistics> getContactStatistics(
            @AuthenticationPrincipal RUser rUser) {
        return execute("get contact statistics", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return contactService.getContactStatistics(restaurantId);
        });
    }
}