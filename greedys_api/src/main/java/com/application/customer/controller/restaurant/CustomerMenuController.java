package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.get.MenuDTO;
import com.application.common.web.dto.get.MenuDishDTO;
import com.application.restaurant.service.RestaurantMenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@RequestMapping("/customer/restaurant")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerMenuController extends BaseController {
    private final RestaurantMenuService restaurantMenuService;

    @Operation(summary = "Get menus by restaurant ID", description = "Retrieve all menus for a specific restaurant by its ID")
    @GetMapping("/{restaurantId}/menus")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<MenuDTO>>> getMenusByRestaurantId(@PathVariable Long restaurantId) {
        return execute("get menus by restaurant", () -> restaurantMenuService.getMenusByRestaurant(restaurantId));
    }

    @Operation(summary = "Get menus with services valid in a period", description = "Retrieve menus for a restaurant with services valid in a given period")
    @GetMapping("/{restaurantId}/menus/period")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<MenuDTO>>> getMenusWithServicesValidInPeriod(
            @PathVariable Long restaurantId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return execute("get menus with services valid in period", () -> restaurantMenuService.getMenusWithServicesValidInPeriod(restaurantId, startDate, endDate));
    }
    
    @Operation(summary = "Get dishes by menu ID", description = "Retrieve all dishes for a specific menu by its ID")
    @GetMapping("/menus/{menuId}/dishes")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<MenuDishDTO>>> getDishesByMenuId(@PathVariable Long menuId) {
        return execute("get dishes by menu", () -> restaurantMenuService.getMenuDishesByMenuId(menuId));
    }

    @Operation(summary = "Get menu details by ID", description = "Retrieve details of a specific menu by its ID")
    @GetMapping("/menus/{menuId}")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<MenuDTO>> getMenuDetailsById(@PathVariable Long menuId) {
        return execute("get menu details", () -> restaurantMenuService.getMenuById(menuId));
    }

    @Operation(summary = "Get menus by service ID that are active and enabled in a date", description = "Retrieve all menus for a specific service that are active and enabled in a given date")
    @GetMapping("/service/{serviceId}/menus/active-enabled")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<MenuDTO>>> getActiveEnabledMenusByServiceId(
            @PathVariable Long serviceId,
            @RequestParam("date") LocalDate date) {
        return execute("get active enabled menus by service", () -> restaurantMenuService.getActiveEnabledMenusByServiceId(serviceId, date));
    }

    @Operation(summary = "Get menus by service ID that are active and enabled in a period", description = "Retrieve all menus for a specific service that are active and enabled in a given period")
    @GetMapping("/service/{serviceId}/menus/active-enabled/period")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<MenuDTO>>> getActiveEnabledMenusByServiceIdAndPeriod(
            @PathVariable Long serviceId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return execute("get active enabled menus by service and period", () -> restaurantMenuService.getActiveEnabledMenusByServiceIdAndPeriod(serviceId, startDate, endDate));
    }
}