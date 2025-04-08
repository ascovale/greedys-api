package com.application.controller.restaurantUser;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.service.RestaurantMenuService;
import com.application.web.dto.get.MenuItemDTO;
import com.application.web.dto.get.PricedMenuItemDTO;
import com.application.web.dto.get.RestaurantMenuDTO;
import com.application.web.dto.post.NewMenuItemDTO;
import com.application.web.dto.post.NewPricedMenuItemDTO;
import com.application.web.dto.post.NewRestaurantMenuDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@SecurityRequirement(name = "restaurantBearerAuth")
@RequestMapping("/restaurant/menu")
@RestController
public class RestaurantMenuController {
    private final RestaurantMenuService restaurantMenuService;

    public RestaurantMenuController(RestaurantMenuService restaurantMenuService) {
        this.restaurantMenuService = restaurantMenuService;
    }

    @Operation(summary = "Get restaurant menus", description = "Retrieve all menus for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menus retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/menus")
    public Collection<RestaurantMenuDTO> getRestaurantMenus() {
        return restaurantMenuService.getMenusByRestaurant(ControllerUtils.getCurrentRestaurant().getId());
    }

    @Operation(summary = "Get menu items", description = "Retrieve all items for a specific menu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid menu ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{menuId}/items")
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
    @GetMapping("/{menuId}")
    public RestaurantMenuDTO getMenuById(@RequestParam Long menuId) {
        return restaurantMenuService.getMenuById(menuId);
    }

    @Operation(summary = "Get restaurant menu items", description = "Retrieve all menu items for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/items/get")
    public Collection<MenuItemDTO> getRestaurantMenuItems() {
        return restaurantMenuService.getMenuItemsByRestaurant(ControllerUtils.getCurrentRestaurant().getId());
    }

    @Operation(summary = "Create a new menu", description = "Create a new menu for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Menu created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/new")
    public void createMenu(@RequestBody NewRestaurantMenuDTO newMenu) {
        restaurantMenuService.addMenu(newMenu);
    }

    @Operation(summary = "Add item to menu", description = "Add a priced menu item to an existing menu")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item added to menu successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/item")
    public void addItemToMenu(@RequestBody NewPricedMenuItemDTO newMenuItem) {
        restaurantMenuService.addPricedMenuItem(newMenuItem);
    }

    @Operation(summary = "Create a new item", description = "Create a new menu item for the current restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/item/new")
    public ResponseEntity<Void> createItem(@RequestBody NewMenuItemDTO newItem) {
        System.out.println(newItem.getName() + " " + newItem.getDescription() + " " + newItem.getRestaurantId());
        restaurantMenuService.addMenuItem(newItem);
        return ResponseEntity.ok().build();
    }

}