package com.application.controller.pub;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantMenuService;
import com.application.web.dto.get.MenuItemDTO;
import com.application.web.dto.get.PricedMenuItemDTO;
import com.application.web.dto.get.RestaurantMenuDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@SecurityRequirement(name = "restaurantBearerAuth")
@RequestMapping("/public/restaurant/")
@RestController
public class PublicMenuController {
    private final RestaurantMenuService restaurantMenuService;

    public PublicMenuController(RestaurantMenuService restaurantMenuService) {
        this.restaurantMenuService = restaurantMenuService;
    }

    @Operation(summary = "Get restaurant menus", description = "Retrieve all menus for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{restaurantId}/menus")
    public Collection<RestaurantMenuDTO> getRestaurantMenus(@RequestParam Long restaurantId) {
        return restaurantMenuService.getMenusByRestaurant(restaurantId);
    }

    @Operation(summary = "Get menu items", description = "Retrieve all items for a specific menu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/menu/{menuId}/items")
    public Collection<PricedMenuItemDTO> getMenuItems(@RequestParam Long menuId) {
        return restaurantMenuService.getMenuItems(menuId);
    }

    @Operation(summary = "Get menu by ID", description = "Retrieve details of a specific menu by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/menu/{menuId}")
    public RestaurantMenuDTO getMenuById(@RequestParam Long menuId) {
        return restaurantMenuService.getMenuById(menuId);
    }

    @Operation(summary = "Get restaurant menu items", description = "Retrieve all menu items for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{restaurantId}/items/get")
    public Collection<MenuItemDTO> getRestaurantMenuItems(@RequestParam Long restaurantId) {
        return restaurantMenuService.getMenuItemsByRestaurant(restaurantId);
    }


}