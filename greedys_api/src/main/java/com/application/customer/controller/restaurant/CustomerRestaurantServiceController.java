package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.service.RestaurantService;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.dto.restaurant.ServiceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Restaurant Services", description = "Controller for handling requests related to restaurant services")
@RequestMapping("/customer/restaurant")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRestaurantServiceController extends BaseController {

    private final RestaurantService restaurantService;

    @GetMapping("/{restaurantId}/active-services-in-date")
    @Operation(summary = "Get active and enabled services of a restaurant for a specific date", description = "Retrieve the services of a restaurant that are active and enabled on a given date")
    
    @WrapperType(dataClass = ServiceDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<ServiceDTO>> getActiveEnabledServicesInDate(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date) {
        return executeList("get active services in date", () -> {
            Collection<ServiceDTO> services = restaurantService.getActiveEnabledServices(restaurantId, date);
            return services instanceof List ? (List<ServiceDTO>) services : List.copyOf(services);
        });
    }

    @GetMapping("/{restaurantId}/active-services-in-period")
    @Operation(summary = "Get active and enabled services of a restaurant for a specific period", description = "Retrieve the services of a restaurant that are active and enabled in a given date range")
    @WrapperType(dataClass = ServiceDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<ServiceDTO>> getActiveEnabledServicesInPeriod(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate start,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate end) {
        return executeList("get active services in period", () -> {
            Collection<ServiceDTO> services = restaurantService.findActiveEnabledServicesInPeriod(restaurantId, start, end);
            return services instanceof List ? (List<ServiceDTO>) services : List.copyOf(services);
        });
    }

    @GetMapping("/{restaurantId}/services")
    @Operation(summary = "Get services of a restaurant", description = "Retrieve all services of a restaurant")
    
    @WrapperType(dataClass = ServiceDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<ServiceDTO>> getServices(@PathVariable Long restaurantId) {
        return executeList("get restaurant services", () -> {
            Collection<ServiceDTO> services = restaurantService.getServices(restaurantId);
            return services instanceof List ? (List<ServiceDTO>) services : List.copyOf(services);
        });
    }
}
