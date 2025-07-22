package com.application.restaurant.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.get.DishDTO;
import com.application.common.web.dto.get.MenuDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.RestaurantMenuService;
import com.application.restaurant.web.post.NewDishDTO;
import com.application.restaurant.web.post.NewMenuDTO;
import com.application.restaurant.web.post.NewMenuDishDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Menu Management", description = "Restaurant Menu Controller APIs")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/restaurant/menu")
@RestController
@RequiredArgsConstructor
@Slf4j
public class RestaurantMenuController {
    private final RestaurantMenuService restaurantMenuService;

    @Operation(summary = "Retrieve all menus", description = "Retrieve all menus for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/all")
    public Collection<MenuDTO> getRestaurantMenus() {
        return restaurantMenuService.getMenusByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId());
    }

    @Operation(summary = "Get dishes of a menu", description = "Retrieve all dishes of a specific menu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dishes retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#menuId, authentication.principal.restaurantId)")
    @GetMapping("/{menuId}/dishes")
    public ResponseEntity<?> getMenuDishes(@PathVariable Long menuId) {
        return ResponseEntity.ok(restaurantMenuService.getMenuDishesByMenuId(menuId));
    }

    @Operation(summary = "Get menu details", description = "Retrieve details of a specific menu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#menuId, authentication.principal.restaurantId)")
    @GetMapping("/{menuId}")
    public ResponseEntity<?> getMenuDetails(@PathVariable Long menuId) {
        return ResponseEntity.ok(restaurantMenuService.getMenuById(menuId));
    }

    @Operation(summary = "Retrieve all dishes", description = "Retrieve all dishes for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dishes retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dishes/all")
    public Collection<DishDTO> getDishes() {
        return restaurantMenuService.getDishesByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId());
    }

    @Operation(summary = "Create a menu", description = "Create a new menu for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Menu created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create")
    public void createMenu(@RequestBody NewMenuDTO newMenu) {
        restaurantMenuService.addMenu(newMenu);
    }

    @Operation(summary = "Add a dish to a menu", description = "Add a priced dish to an existing menu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dish added to menu successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#newMenuItem.menuId, authentication.principal.restaurantId)")
    @PostMapping("/dishes/add")
    public void addDishToMenu(@RequestBody NewMenuDishDTO newMenuItem) {
        restaurantMenuService.addMenuDish(newMenuItem);
    }

    @Operation(summary = "Create a dish", description = "Create a new dish for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dish created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/dishes/create")
    public ResponseEntity<Void> createDish(@RequestBody NewDishDTO newItem) {
        restaurantMenuService.createDish(newItem);
        return ResponseEntity.ok().build();
    }

}