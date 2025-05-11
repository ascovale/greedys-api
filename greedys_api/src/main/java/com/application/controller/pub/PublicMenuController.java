package com.application.controller.pub;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantMenuService;
import com.application.web.dto.get.MenuDTO;
import com.application.web.dto.get.MenuDishDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.websocket.server.PathParam;

@Tag(name = "2. Menu", description = "Restaurant Menu Controller APIs")
@SecurityRequirement(name = "restaurantBearerAuth")
@RequestMapping("/public/restaurant")
@RestController
public class PublicMenuController {
    private final RestaurantMenuService restaurantMenuService;

    public PublicMenuController(RestaurantMenuService restaurantMenuService) {
        this.restaurantMenuService = restaurantMenuService;
    }

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
}