package com.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantMenuService;
import com.application.web.dto.get.MenuItemDTO;
import com.application.web.dto.get.PricedMenuItemDTO;
import com.application.web.dto.get.RestaurantMenuDTO;
import com.application.web.dto.post.NewMenuItemDTO;
import com.application.web.dto.post.NewPricedMenuItemDTO;
import com.application.web.dto.post.NewRestaurantMenuDTO;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Collection;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class RestaurantMenuController {

    private final RestaurantMenuService restaurantMenuService;

    public RestaurantMenuController(RestaurantMenuService restaurantMenuService) {
        this.restaurantMenuService = restaurantMenuService;
    }

    @GetMapping("/restaurant/menu")
    public Collection<RestaurantMenuDTO> getRestaurantMenus( @RequestParam Long id) {
        return restaurantMenuService.getMenusByRestaurant(id);
    }

    @GetMapping("/menu/item")
    public Collection<PricedMenuItemDTO> getMenuItems( @RequestParam Long id) {
        return restaurantMenuService.getMenuItems(id);
    }

    @GetMapping("/restaurant/item")
    public Collection<MenuItemDTO> getRestaurantMenuItems( @RequestParam Long id) {
        return restaurantMenuService.getMenuItemsByRestaurant(id);
    }

    @PostMapping("/menu")
    public void createMenu(@RequestBody NewRestaurantMenuDTO newMenu) {
        restaurantMenuService.addMenu(newMenu);
    }

    @PostMapping("/menu/item")
    public void addItemToMenu(@RequestBody NewPricedMenuItemDTO newMenuItem) {
        restaurantMenuService.addPricedMenuItem(newMenuItem);
    }

    @PostMapping("/item")
    public ResponseEntity<Void> createItem(@RequestBody NewMenuItemDTO newItem) {
        restaurantMenuService.addMenuItem(newItem);
        return ResponseEntity.ok().build();
    }

}
