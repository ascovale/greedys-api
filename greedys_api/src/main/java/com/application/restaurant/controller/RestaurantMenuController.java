package com.application.restaurant.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.menu.DishDTO;
import com.application.common.web.dto.menu.MenuDTO;
import com.application.common.web.dto.menu.MenuDishDTO;
import com.application.restaurant.controller.utils.RestaurantControllerUtils;
import com.application.restaurant.service.RestaurantMenuService;
import com.application.restaurant.web.dto.menu.NewDishDTO;
import com.application.restaurant.web.dto.menu.NewMenuDTO;
import com.application.restaurant.web.dto.menu.NewMenuDishDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @ApiResponse(responseCode = "200", description = "Menus retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = MenuDTO.class))))
    @ReadApiResponses
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public ListResponseWrapper<MenuDTO> getRestaurantMenus() {
        return executeList("get restaurant menus", () -> {
            Collection<MenuDTO> menus = restaurantMenuService.getMenusByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId());
            return menus instanceof List ? (List<MenuDTO>) menus : new java.util.ArrayList<>(menus);
        });
    }

    @Operation(summary = "Get dishes of a menu", description = "Retrieve all dishes of a specific menu")
    @ApiResponse(responseCode = "200", description = "Menu dishes retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = MenuDishDTO.class))))
    @ReadApiResponses
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#menuId, authentication.principal.restaurantId)")
    @GetMapping("/{menuId}/dishes")
    @ResponseStatus(HttpStatus.OK)
    public ListResponseWrapper<MenuDishDTO> getMenuDishes(@PathVariable Long menuId) {
        return executeList("get menu dishes", () -> restaurantMenuService.getMenuDishesByMenuId(menuId));
    }

    @Operation(summary = "Get menu details", description = "Retrieve details of a specific menu")
    @ApiResponse(responseCode = "200", description = "Menu details retrieved successfully", 
                content = @Content(schema = @Schema(implementation = MenuDTO.class)))
    @ReadApiResponses
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#menuId, authentication.principal.restaurantId)")
    @GetMapping("/{menuId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseWrapper<MenuDTO> getMenuDetails(@PathVariable Long menuId) {
        return execute("get menu details", () -> restaurantMenuService.getMenuById(menuId));
    }

    @Operation(summary = "Retrieve all dishes", description = "Retrieve all dishes for the current restaurant")
    @ApiResponse(responseCode = "200", description = "Restaurant dishes retrieved successfully", 
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = DishDTO.class))))
    @ReadApiResponses
    @GetMapping("/dishes/all")
    @ResponseStatus(HttpStatus.OK)
    public ListResponseWrapper<DishDTO> getDishes() {
        return executeList("get restaurant dishes", () -> {
            Collection<DishDTO> dishes = restaurantMenuService.getDishesByRestaurant(RestaurantControllerUtils.getCurrentRestaurant().getId());
            return dishes instanceof List ? (List<DishDTO>) dishes : new java.util.ArrayList<>(dishes);
        });
    }

    @Operation(summary = "Create a menu", description = "Create a new menu for the current restaurant")
    @ApiResponse(responseCode = "201", description = "Menu created successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @CreateApiResponses
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseWrapper<String> createMenu(@RequestBody NewMenuDTO newMenu) {
        return executeCreate("create menu", "Menu created successfully", () -> {
            restaurantMenuService.addMenu(newMenu);
            return "success";
        });
    }

    @Operation(summary = "Add a dish to a menu", description = "Add a priced dish to an existing menu")
    @ApiResponse(responseCode = "201", description = "Dish added to menu successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @CreateApiResponses
    @PreAuthorize("@securityRUserService.isMenuOwnedByRestaurant(#newMenuItem.menuId, authentication.principal.restaurantId)")
    @PostMapping("/dishes/add")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseWrapper<String> addDishToMenu(@RequestBody NewMenuDishDTO newMenuItem) {
        return executeCreate("add dish to menu", "Dish added to menu successfully", () -> {
            restaurantMenuService.addMenuDish(newMenuItem);
            return "success";
        });
    }

    @Operation(summary = "Create a dish", description = "Create a new dish for the current restaurant")
    @ApiResponse(responseCode = "201", description = "Dish created successfully", 
                content = @Content(schema = @Schema(implementation = String.class)))
    @CreateApiResponses
    @PostMapping("/dishes/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseWrapper<String> createDish(@RequestBody NewDishDTO newItem) {
        return executeCreate("create dish", "Dish created successfully", () -> {
            restaurantMenuService.createDish(newItem);
            return "success";
        });
    }
}
