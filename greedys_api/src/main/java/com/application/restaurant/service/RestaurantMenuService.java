package com.application.restaurant.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.DishMapper;
import com.application.common.persistence.mapper.MenuDishMapper;
import com.application.common.persistence.mapper.MenuMapper;
import com.application.common.web.dto.menu.DishDTO;
import com.application.common.web.dto.menu.MenuDTO;
import com.application.common.web.dto.menu.MenuDishDTO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.menu.DishDAO;
import com.application.restaurant.persistence.dao.menu.MenuDAO;
import com.application.restaurant.persistence.dao.menu.MenuDishDAO;
import com.application.restaurant.persistence.model.menu.Dish;
import com.application.restaurant.persistence.model.menu.Menu;
import com.application.restaurant.persistence.model.menu.MenuDish;
import com.application.restaurant.web.dto.menu.NewDishDTO;
import com.application.restaurant.web.dto.menu.NewMenuDTO;
import com.application.restaurant.web.dto.menu.NewMenuDishDTO;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@Transactional
@RequiredArgsConstructor
public class RestaurantMenuService {

    private final DishDAO dishDAO;
    private final MenuDAO menuDAO;
    private final ServiceDAO serviceDAO;
    private final RestaurantDAO restaurantDAO;
    private final MenuDishDAO menuDishDAO;
    
    private final MenuMapper menuMapper;
    private final DishMapper dishMapper;
    private final MenuDishMapper menuDishMapper;

    public Collection<MenuDTO> getAllMenus() {
        return menuDAO.findAll().stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    public MenuDTO getMenuById(Long id) {
        Menu menu = menuDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + id));
        return menuMapper.toDTO(menu);
    }

    public List<MenuDishDTO> getMenuDishesByMenuId(Long menuId) {
        return menuDishDAO.findByMenuId(menuId).stream()
                .map(menuDishMapper::toDTO)
                .toList();
    }

    public Collection<DishDTO> getDishesByRestaurant(Long id) {
        return dishDAO.findByRestaurantId(id).stream()
                .map(dishMapper::toDTO)
                .toList();
    }


    public Collection<MenuDTO> getActiveMenusByRestaurant(Long id) {
        return menuDAO.findByRestaurantId(id).stream()
                .map(menuMapper::toDTO)
                .toList();
    }
    
    public Collection<MenuDTO> getMenusByRestaurant(Long id) {
        return menuDAO.findByRestaurantId(id).stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    public void addMenuDish(NewMenuDishDTO menuDishDTO) {
        Dish dish = dishDAO.getReferenceById(menuDishDTO.getDishId());
        Menu menu = menuDAO.getReferenceById(menuDishDTO.getMenuId());
        MenuDish menuDish = MenuDish.builder()
                .menu(menu)
                .dish(dish)
                .price(menuDishDTO.getPrice())
                .build();
        menuDishDAO.save(menuDish);
    }

    public void createDish(NewDishDTO menuItem) {
        Dish item = Dish.builder()
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .restaurant(restaurantDAO.getReferenceById(menuItem.getRestaurantId()))
                .build();
        // item.setAllergens(menuItem.getAllergen());
        dishDAO.save(item);
    }

    public void addMenu(NewMenuDTO menu) {
        Menu newMenu = Menu.builder()
                .name(menu.getName())
                .description(menu.getDescription())
                .services(menu.getServiceIds().stream()
                        .map(serviceId -> serviceDAO.getReferenceById(serviceId))
                        .toList())
                .build();
        menuDAO.save(newMenu);
    }

    public void deleteMenuItem(Long id) {
        dishDAO.deleteById(id);
    }

    public void deleteMenuDish(Long menuDishId) {
        menuDishDAO.deleteById(menuDishId);
    }

    public void deleteMenu(Long id) {
        menuDAO.deleteById(id);
    }

    public Collection<MenuDTO> getMenusWithServicesValidInPeriod(Long restaurantId, LocalDate startDate, LocalDate endDate) {
        return menuDAO.findMenusWithServicesValidInPeriod(restaurantId, startDate, endDate)
                .stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    public Collection<MenuDTO> getActiveEnabledMenusByServiceId(Long serviceId, LocalDate date) {
        return menuDAO.findActiveEnabledMenusByServiceIdAndDate(serviceId, date)
                .stream()
                .map(menuMapper::toDTO)
                .toList();
    }

    public Collection<MenuDTO> getActiveEnabledMenusByServiceIdAndPeriod(Long serviceId, LocalDate startDate, LocalDate endDate) {
        return menuDAO.findActiveEnabledMenusByServiceIdAndPeriod(serviceId, startDate, endDate)
                .stream()
                .map(menuMapper::toDTO)
                .toList();
    }

}
