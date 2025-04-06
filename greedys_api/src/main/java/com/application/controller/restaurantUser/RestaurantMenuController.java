package com.application.controller.restaurantUser;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.controller.utils.ControllerUtils;
import com.application.service.RestaurantMenuService;
import com.application.web.dto.get.MenuItemDTO;
import com.application.web.dto.get.PricedMenuItemDTO;
import com.application.web.dto.get.RestaurantMenuDTO;
import com.application.web.dto.post.NewMenuItemDTO;
import com.application.web.dto.post.NewPricedMenuItemDTO;
import com.application.web.dto.post.NewRestaurantMenuDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@SecurityRequirement(name = "restaurantBearerAuth")
@RequestMapping("/restaurant/menu")
@RestController
public class RestaurantMenuController {

    // TODO: sistemare i metodi
    private final RestaurantMenuService restaurantMenuService;

    public RestaurantMenuController(RestaurantMenuService restaurantMenuService) {
        this.restaurantMenuService = restaurantMenuService;
    }

    @GetMapping("/restaurant/menu")
    public Collection<RestaurantMenuDTO> getRestaurantMenus() {
        return restaurantMenuService.getMenusByRestaurant(ControllerUtils.getCurrentRestaurant().getId());
    }

    @GetMapping("/menu/item")
    public Collection<PricedMenuItemDTO> getMenuItems() {
        return restaurantMenuService.getMenuItems(ControllerUtils.getCurrentRestaurant().getId());
    }

    @GetMapping("/restaurant/item")
    public Collection<MenuItemDTO> getRestaurantMenuItems() {
        return restaurantMenuService.getMenuItemsByRestaurant(ControllerUtils.getCurrentRestaurant().getId());
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
        System.out.println(newItem.getName() + " " + newItem.getDescription() + " " + newItem.getRestaurantId());
        restaurantMenuService.addMenuItem(newItem);
        return ResponseEntity.ok().build();
    }

}
