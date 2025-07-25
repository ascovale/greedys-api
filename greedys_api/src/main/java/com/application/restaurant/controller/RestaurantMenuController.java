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

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ApiResponse;
import com.application.common.web.dto.get.DishDTO;
import com.application.common.web.dto.get.MenuDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.RestaurantMenuService;
import com.application.restaurant.web.dto.post.NewDishDTO;
import com.application.restaurant.web.dto.post.NewMenuDTO;
import com.application.restaurant.web.dto.post.NewMenuDishDTO;

import io.swagger.v3.oas.annotations.Operation;
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
public class RestaurantMenuController extends BaseController {
    private final RestaurantMenuService restaurantMenuService;

    @Operation(summary = "Retrieve all menus", description = "Retrieve all menus for the current restaurant")
    @GetMapping("/all")
    @ReadApiResponses
    public ResponseEntity<ApiResponse<Collection<MenuDTO>>> getRestaurantMenus() {
        return execute("get restaurant menus", () -> 
            restaurantMenuService.getMenusByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId()));
    }

    @Operation(summary = "Get dishes of a menu", description = "Retrieve all dishes of a specific menu")
    @ReadApiResponses
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#menuId, authentication.principal.restaurantId)")
    @GetMapping("/{menuId}/dishes")
    public ResponseEntity<ApiResponse<Object>> getMenuDishes(@PathVariable Long menuId) {
        return execute("get menu dishes", () -> restaurantMenuService.getMenuDishesByMenuId(menuId));
    }

    @Operation(summary = "Get menu details", description = "Retrieve details of a specific menu")
    @ReadApiResponses
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#menuId, authentication.principal.restaurantId)")
    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Object>> getMenuDetails(@PathVariable Long menuId) {
        return execute("get menu details", () -> restaurantMenuService.getMenuById(menuId));
    }

    @Operation(summary = "Retrieve all dishes", description = "Retrieve all dishes for the current restaurant")
    @GetMapping("/dishes/all")
    public ResponseEntity<ApiResponse<Collection<DishDTO>>> getDishes() {
        return execute("get restaurant dishes", () -> 
            restaurantMenuService.getDishesByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId()));
    }

    @Operation(summary = "Create a menu", description = "Create a new menu for the current restaurant")
    @CreateApiResponses
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> createMenu(@RequestBody NewMenuDTO newMenu) {
        return executeCreate("create menu", "Menu created successfully", () -> {
            restaurantMenuService.addMenu(newMenu);
            return "success";
        });
    }

    @Operation(summary = "Add a dish to a menu", description = "Add a priced dish to an existing menu")
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#newMenuItem.menuId, authentication.principal.restaurantId)")
    @PostMapping("/dishes/add")
    public ResponseEntity<ApiResponse<String>> addDishToMenu(@RequestBody NewMenuDishDTO newMenuItem) {
        return executeCreate("add dish to menu", "Dish added to menu successfully", () -> {
            restaurantMenuService.addMenuDish(newMenuItem);
            return "success";
        });
    }

    @Operation(summary = "Create a dish", description = "Create a new dish for the current restaurant")
    @CreateApiResponses
    @PostMapping("/dishes/create")
    public ResponseEntity<ApiResponse<String>> createDish(@RequestBody NewDishDTO newItem) {
        return executeCreate("create dish", "Dish created successfully", () -> {
            restaurantMenuService.createDish(newItem);
            return "success";
        });
    }
}