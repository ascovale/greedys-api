package com.application.restaurant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.customer.CustomerStatisticsDTO;
import com.application.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Customer", description = "Controller for restaurant operators to manage customer data")
@RestController
@RequestMapping("/restaurant/customer")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantCustomerController extends BaseController {
    
    private final CustomerService customerService;

    @Operation(summary = "Get customer statistics", description = "Retrieves statistics for a specific customer including no-show rate, reservations count, etc.")
    @ApiResponse(responseCode = "200", description = "Customer statistics retrieved successfully", 
                content = @Content(schema = @Schema(implementation = CustomerStatisticsDTO.class)))
    @GetMapping("/{idCustomer}/statistics")
    @ReadApiResponses
    public ResponseEntity<ResponseWrapper<CustomerStatisticsDTO>> getCustomerStatistics(
            @Parameter(description = "Customer ID", required = true, example = "1")
            @PathVariable Long idCustomer) {
        return execute("get customer statistics", () -> customerService.getCustomerStatistics(idCustomer));
    }
}
