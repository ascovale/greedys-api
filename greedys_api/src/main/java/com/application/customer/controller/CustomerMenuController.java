package com.application.customer.controller;

/**
 * TODO: CLEANUP - Actions to do for the customer MenuController
 * 1. This controller is now public/without authentication, so all endpoints can remain here.
 * 2. Ensure that methods returning menus and dishes filter data based on customer visibility (only public/available data).
 * 3. Remove or limit methods exposing administrative or non-public data.
 * 4. Update method documentation to clarify the visibility of returned data.
 * 5. Consider adding additional security/visibility checks where necessary. for example restaurant is open or closed temporarily, menu is active, etc.
 */

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.MenuDTO;
import com.application.common.web.dto.get.MenuDishDTO;
import com.application.restaurant.service.RestaurantMenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@RequestMapping("/customer/restaurant")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class CustomerMenuController {
    private final RestaurantMenuService restaurantMenuService;

    public CustomerMenuController(RestaurantMenuService restaurantMenuService) {
        this.restaurantMenuService = restaurantMenuService;
    }

    //TODO add pagination to the menus list ??? non so se ha senso


    @Operation(summary = "Get menus by restaurant ID", description = "Retrieve all menus for a specific restaurant by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MenuDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{restaurantId}/menus")
    public Collection<MenuDTO> getMenusByRestaurantId(@PathVariable Long restaurantId) {
        return restaurantMenuService.getMenusByRestaurant(restaurantId);
    }

    @Operation(summary = "Get menus with services valid in a period", description = "Retrieve menus for a restaurant with services valid in a given period")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MenuDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{restaurantId}/menus/period")
    public Collection<MenuDTO> getMenusWithServicesValidInPeriod(
            @PathVariable Long restaurantId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return restaurantMenuService.getMenusWithServicesValidInPeriod(restaurantId, startDate, endDate);
    }

    
    @Operation(summary = "Get dishes by menu ID", description = "Retrieve all dishes for a specific menu by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dishes retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MenuDishDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/menus/{menuId}/dishes")
    public Collection<MenuDishDTO> getDishesByMenuId(@PathVariable Long menuId) {
        return restaurantMenuService.getMenuDishesByMenuId(menuId);
    }

    @Operation(summary = "Get menu details by ID", description = "Retrieve details of a specific menu by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu details retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MenuDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/menus/{menuId}")
    public MenuDTO getMenuDetailsById(@PathVariable Long menuId) {
        return restaurantMenuService.getMenuById(menuId);
    }

    @Operation(summary = "Get menus by service ID that are active and enabled in a date", description = "Retrieve all menus for a specific service that are active and enabled in a given date")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MenuDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid service ID or date"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/service/{serviceId}/menus/active-enabled")
    public Collection<MenuDTO> getActiveEnabledMenusByServiceId(
            @PathVariable Long serviceId,
            @RequestParam("date") LocalDate date) {
        return restaurantMenuService.getActiveEnabledMenusByServiceId(serviceId, date);
    }

    @Operation(summary = "Get menus by service ID that are active and enabled in a period", description = "Retrieve all menus for a specific service that are active and enabled in a given period")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MenuDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid service ID or period"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/service/{serviceId}/menus/active-enabled/period")
    public Collection<MenuDTO> getActiveEnabledMenusByServiceIdAndPeriod(
            @PathVariable Long serviceId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return restaurantMenuService.getActiveEnabledMenusByServiceIdAndPeriod(serviceId, startDate, endDate);
    }
}