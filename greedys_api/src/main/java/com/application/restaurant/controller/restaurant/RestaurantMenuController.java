package com.application.restaurant.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.menu.DishDTO;
import com.application.common.web.dto.menu.MenuDTO;
import com.application.common.web.dto.menu.MenuDishDTO;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.RestaurantMenuService;
import com.application.restaurant.web.dto.menu.NewDishDTO;
import com.application.restaurant.web.dto.menu.NewMenuDTO;
import com.application.restaurant.web.dto.menu.NewMenuDishDTO;

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
    @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<MenuDTO>> getRestaurantMenus(@AuthenticationPrincipal RUser rUser) {
        return executeList("get restaurant menus", () -> {
            Collection<MenuDTO> menus = restaurantMenuService.getMenusByRestaurant(rUser.getRestaurant().getId());
            return menus instanceof List ? (List<MenuDTO>) menus : new java.util.ArrayList<>(menus);
        });
    }

    @Operation(summary = "Get dishes of a menu", description = "Retrieve all dishes of a specific menu")
    
    @PreAuthorize("@securityRUserService.isMenuOwnedByAuthenticatedUser(#menuId)")
    @GetMapping("/{menuId}/dishes")
        @WrapperType(dataClass = MenuDishDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<MenuDishDTO>> getMenuDishes(@PathVariable Long menuId) {
        return executeList("get menu dishes", () -> restaurantMenuService.getMenuDishesByMenuId(menuId));
    }

    @Operation(summary = "Get menu details", description = "Retrieve details of a specific menu")
    
    @PreAuthorize("@securityRUserService.isMenuOwnedByAuthenticatedUser(#menuId)")
    @GetMapping("/{menuId}")
        @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<MenuDTO>> getMenuDetails(@PathVariable Long menuId) {
        return execute("get menu details", () -> restaurantMenuService.getMenuById(menuId));
    }

    @Operation(summary = "Retrieve all dishes", description = "Retrieve all dishes for the current restaurant")
    @GetMapping("/dishes/all")
        @WrapperType(dataClass = DishDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ListResponseWrapper<DishDTO>> getDishes(@AuthenticationPrincipal RUser rUser) {
        return executeList("get restaurant dishes", () -> {
            Collection<DishDTO> dishes = restaurantMenuService.getDishesByRestaurant(rUser.getRestaurant().getId());
            return dishes instanceof List ? (List<DishDTO>) dishes : new java.util.ArrayList<>(dishes);
        });
    }

    @Operation(summary = "Create a menu", description = "Create a new menu for the current restaurant")
    
    @PostMapping("/create")
    @WrapperType(dataClass = MenuDTO.class, responseCode = "201") 
    
    public ResponseEntity<ResponseWrapper<MenuDTO>> createMenu(@RequestBody NewMenuDTO newMenu) {
        return executeCreate("create menu", "Menu created successfully", () -> {
            return restaurantMenuService.addMenu(newMenu);
        });
    }

    @Operation(summary = "Add a dish to a menu", description = "Add a priced dish to an existing menu")
    
    @PreAuthorize("@securityRUserService.isMenuOwnedByAuthenticatedUser(#newMenuItem.menuId)")
    @PostMapping("/dishes/add")
    @WrapperType(dataClass = MenuDishDTO.class, responseCode = "201")
    public ResponseEntity<ResponseWrapper<MenuDishDTO>> addDishToMenu(@RequestBody NewMenuDishDTO newMenuItem) {
        return executeCreate("add dish to menu", "Dish added to menu successfully", () -> {
            return restaurantMenuService.addMenuDish(newMenuItem);
        });
    }

    @Operation(summary = "Create a dish", description = "Create a new dish for the current restaurant")
    @PostMapping("/dishes/create")
    @WrapperType(dataClass = DishDTO.class, responseCode = "201")  
    public ResponseEntity<ResponseWrapper<DishDTO>> createDish(@RequestBody NewDishDTO newItem, @AuthenticationPrincipal RUser rUser) {
        return executeCreate("create dish", "Dish created successfully", () -> {
            return restaurantMenuService.createDish(newItem, rUser.getRestaurant().getId());
        });
    }
}
