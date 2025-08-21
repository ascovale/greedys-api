package com.application.customer.controller.restaurant;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ResponseWrapper;
import com.application.common.web.dto.menu.MenuDTO;
import com.application.common.web.dto.menu.MenuDishDTO;
import com.application.restaurant.service.RestaurantMenuService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Menu", description = "Restaurant Menu Controller APIs")
@RequestMapping("/customer/restaurant")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerMenuController extends BaseController {
    private final RestaurantMenuService restaurantMenuService;

    @Operation(summary = "Get menus by restaurant ID", description = "Retrieve all menus for a specific restaurant by its ID")
    @GetMapping("/{restaurantId}/menus")
    
    @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<MenuDTO>>> getMenusByRestaurantId(@PathVariable Long restaurantId) {
        return executeList("get menus by restaurant", () -> {
            Collection<MenuDTO> menus = restaurantMenuService.getMenusByRestaurant(restaurantId);
            return menus instanceof List ? (List<MenuDTO>) menus : List.copyOf(menus);
        });
    }

    @Operation(summary = "Get menus with services valid in a period", description = "Retrieve menus for a restaurant with services valid in a given period")
    @GetMapping("/{restaurantId}/menus/period")
    
    @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<MenuDTO>>> getMenusWithServicesValidInPeriod(
            @PathVariable Long restaurantId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return executeList("get menus with services valid in period", () -> {
            Collection<MenuDTO> menus = restaurantMenuService.getMenusWithServicesValidInPeriod(restaurantId, startDate, endDate);
            return menus instanceof List ? (List<MenuDTO>) menus : List.copyOf(menus);
        });
    }
    
    @Operation(summary = "Get dishes by menu ID", description = "Retrieve all dishes for a specific menu by its ID")
    @GetMapping("/menus/{menuId}/dishes")
    
    @WrapperType(dataClass = MenuDishDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<MenuDishDTO>>> getDishesByMenuId(@PathVariable Long menuId) {
        return executeList("get dishes by menu", () -> {
            Collection<MenuDishDTO> dishes = restaurantMenuService.getMenuDishesByMenuId(menuId);
            return dishes instanceof List ? (List<MenuDishDTO>) dishes : List.copyOf(dishes);
        });
    }

    @Operation(summary = "Get menu details by ID", description = "Retrieve details of a specific menu by its ID")
    @GetMapping("/menus/{menuId}")
    
    @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.DTO)
    public ResponseEntity<ResponseWrapper<MenuDTO>> getMenuDetailsById(@PathVariable Long menuId) {
        return execute("get menu details", () -> restaurantMenuService.getMenuById(menuId));
    }

    @Operation(summary = "Get menus by service ID that are active and enabled in a date", description = "Retrieve all menus for a specific service that are active and enabled in a given date")
    @GetMapping("/service/{serviceId}/menus/active-enabled")
    
    @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<MenuDTO>>> getActiveEnabledMenusByServiceId(
            @PathVariable Long serviceId,
            @RequestParam("date") LocalDate date) {
        return executeList("get active enabled menus by service", () -> {
            Collection<MenuDTO> menus = restaurantMenuService.getActiveEnabledMenusByServiceId(serviceId, date);
            return menus instanceof List ? (List<MenuDTO>) menus : List.copyOf(menus);
        });
    }

    @Operation(summary = "Get menus by service ID that are active and enabled in a period", description = "Retrieve all menus for a specific service that are active and enabled in a given period")
    @GetMapping("/service/{serviceId}/menus/active-enabled/period")
    
    @WrapperType(dataClass = MenuDTO.class, type = WrapperDataType.LIST)
    public ResponseEntity<ResponseWrapper<List<MenuDTO>>> getActiveEnabledMenusByServiceIdAndPeriod(
            @PathVariable Long serviceId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return executeList("get active enabled menus by service and period", () -> {
            Collection<MenuDTO> menus = restaurantMenuService.getActiveEnabledMenusByServiceIdAndPeriod(serviceId, startDate, endDate);
            return menus instanceof List ? (List<MenuDTO>) menus : List.copyOf(menus);
        });
    }
}
