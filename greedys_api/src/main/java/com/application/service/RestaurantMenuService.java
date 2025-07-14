package com.application.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.application.persistence.dao.menu.DishDAO;
import com.application.persistence.dao.menu.MenuDAO;
import com.application.persistence.dao.menu.MenuDishDAO;
import com.application.persistence.model.menu.Dish;
import com.application.persistence.model.menu.Menu;
import com.application.persistence.model.menu.MenuDish;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.web.dto.get.DishDTO;
import com.application.web.dto.get.MenuDTO;
import com.application.web.dto.get.MenuDishDTO;
import com.application.web.dto.post.NewDishDTO;
import com.application.web.dto.post.NewMenuDTO;
import com.application.web.dto.post.NewMenuDishDTO;

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
        MenuDish menuDish = new MenuDish(menu, dish);
        menuDish.setPrice(menuDishDTO.getPrice());
        menuDishDAO.save(menuDish);
    }

    public void createDish(NewDishDTO menuItem) {

        Dish item = new Dish();
        item.setName(menuItem.getName());
        item.setDescription(menuItem.getDescription());
        // item.setAllergens(menuItem.getAllergen());
        item.setRestaurant(entityManager.getReference(Restaurant.class, menuItem.getRestaurantId()));
        dishDAO.save(item);
    }

    public void addMenu(NewMenuDTO menu) {
        Menu newMenu = new Menu();
        newMenu.setName(menu.getName());
        newMenu.setDescription(menu.getDescription());
        newMenu.setServices(menu.getServiceIds().stream()
                .map(serviceId -> entityManager.getReference(Service.class, serviceId))
                .toList());
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
