package com.application.restaurant.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.application.common.persistence.model.reservation.Service;
import com.application.common.web.dto.get.DishDTO;
import com.application.common.web.dto.get.MenuDTO;
import com.application.common.web.dto.get.MenuDishDTO;
import com.application.restaurant.dao.menu.DishDAO;
import com.application.restaurant.dao.menu.MenuDAO;
import com.application.restaurant.dao.menu.MenuDishDAO;
import com.application.restaurant.model.Restaurant;
import com.application.restaurant.model.menu.Dish;
import com.application.restaurant.model.menu.Menu;
import com.application.restaurant.model.menu.MenuDish;
import com.application.restaurant.web.post.NewDishDTO;
import com.application.restaurant.web.post.NewMenuDTO;
import com.application.restaurant.web.post.NewMenuDishDTO;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class RestaurantMenuService {

    private final EntityManager entityManager;

    private final DishDAO dishDAO;

    private final MenuDAO menuDAO;

    private final MenuDishDAO menuDishDAO;

    public RestaurantMenuService(MenuDAO menuDAO, MenuDishDAO menuDishDAO, EntityManager entityManager,
            DishDAO dishDAO) {
        this.entityManager = entityManager;
        this.menuDAO = menuDAO;
        this.menuDishDAO = menuDishDAO;
        this.dishDAO = dishDAO;
    }

    public Collection<MenuDTO> getAllMenus() {
        return menuDAO.findAll().stream().map(MenuDTO::new).toList();
    }

    public MenuDTO getMenuById(Long id) {
        return new MenuDTO(menuDAO.findById(id).orElseThrow());
    }

    public List<MenuDishDTO> getMenuDishesByMenuId(Long menuId) {

        return menuDishDAO.findByMenuId(menuId).stream().map(MenuDishDTO::new).toList();
    }

    public Collection<DishDTO> getDishesByRestaurant(Long id) {
        return dishDAO.findByRestaurantId(id).stream().map(DishDTO::new).toList();
    }


    public Collection<MenuDTO> getActiveMenusByRestaurant(Long id) {
        return menuDAO.findByRestaurantId(id).stream().map(MenuDTO::new).toList();
    }
    public Collection<MenuDTO> getMenusByRestaurant(Long id) {
        return menuDAO.findByRestaurantId(id).stream().map(MenuDTO::new).toList();
    }

    public void addMenuDish(NewMenuDishDTO menuDishDTO) {
        Dish dish = entityManager.getReference(Dish.class, menuDishDTO.getDishId());
        Menu menu = entityManager.getReference(Menu.class, menuDishDTO.getMenuId());
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
                .restaurant(entityManager.getReference(Restaurant.class, menuItem.getRestaurantId()))
                .build();
        // item.setAllergens(menuItem.getAllergen());
        dishDAO.save(item);
    }

    public void addMenu(NewMenuDTO menu) {
        Menu newMenu = Menu.builder()
                .name(menu.getName())
                .description(menu.getDescription())
                .services(menu.getServiceIds().stream()
                        .map(serviceId -> entityManager.getReference(Service.class, serviceId))
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
                .map(MenuDTO::new)
                .toList();
    }

    public Collection<MenuDTO> getActiveEnabledMenusByServiceId(Long serviceId, LocalDate date) {
        return menuDAO.findActiveEnabledMenusByServiceIdAndDate(serviceId, date)
                .stream()
                .map(MenuDTO::new)
                .toList();
    }

    public Collection<MenuDTO> getActiveEnabledMenusByServiceIdAndPeriod(Long serviceId, LocalDate startDate, LocalDate endDate) {
        return menuDAO.findActiveEnabledMenusByServiceIdAndPeriod(serviceId, startDate, endDate)
                .stream()
                .map(MenuDTO::new)
                .toList();
    }

}
