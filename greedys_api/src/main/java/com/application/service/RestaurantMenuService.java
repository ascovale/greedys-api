package com.application.service;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.menu.MenuItemDAO;
import com.application.persistence.dao.menu.PricedMenuItemDAO;
import com.application.persistence.dao.menu.RestaurantMenuDAO;
import com.application.persistence.model.menu.MenuHasItem;
import com.application.persistence.model.menu.MenuHasItemId;
import com.application.persistence.model.menu.MenuItem;
import com.application.persistence.model.menu.RestaurantMenu;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.web.dto.get.MenuItemDTO;
import com.application.web.dto.get.PricedMenuItemDTO;
import com.application.web.dto.get.RestaurantMenuDTO;
import com.application.web.dto.post.NewMenuItemDTO;
import com.application.web.dto.post.NewPricedMenuItemDTO;
import com.application.web.dto.post.NewRestaurantMenuDTO;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Collection;

@Service
@Transactional
public class RestaurantMenuService {

    private final EntityManager entityManager;

    private final RestaurantMenuDAO menuDAO;

    private final MenuItemDAO menuItemDAO;

    private final PricedMenuItemDAO pricedMenuItemDAO;

    public RestaurantMenuService(RestaurantMenuDAO menuDAO, PricedMenuItemDAO pricedMenuItemDAO, EntityManager entityManager, MenuItemDAO menuItemDAO) {
        this.entityManager = entityManager;
        this.menuDAO = menuDAO;
        this.menuItemDAO = menuItemDAO;
        this.pricedMenuItemDAO = pricedMenuItemDAO;
    }

    public Collection<RestaurantMenuDTO> getAllMenus() {
        return menuDAO.findAll().stream().map(RestaurantMenuDTO::new).toList();
    }

    public RestaurantMenuDTO getMenuById(Long id) {
        return new RestaurantMenuDTO(menuDAO.findById(id).orElseThrow());
    }

    public Collection<PricedMenuItemDTO> getMenuItems(Long id) {
        return pricedMenuItemDAO.findPricedItemsByMenuId(id);
    }

    public Collection<MenuItemDTO> getMenuItemsByRestaurant(Long id) {
        return menuItemDAO.findByRestaurant(id).stream().map(MenuItemDTO::new).toList();
    }

    public Collection<RestaurantMenuDTO> getMenusByRestaurant(Long id) {
        return menuDAO.findByRestaurantId(id).stream().map(RestaurantMenuDTO::new).toList();
    }

    public void addPricedMenuItem(NewPricedMenuItemDTO menuItem) {
        MenuItem item = entityManager.getReference(MenuItem.class, menuItem.getItemId());
        RestaurantMenu menu = entityManager.getReference(RestaurantMenu.class, menuItem.getMenuId());
        MenuHasItem menuHasItem = new MenuHasItem(
            item,
            menu
        );
        menuHasItem.setPrice(menuItem.getPrice());
        pricedMenuItemDAO.save(menuHasItem);
    }

    public void addMenuItem(NewMenuItemDTO menuItem) {

        System.out.println("Adding menu item" + menuItem.getName() + " " + menuItem.getDescription() + " " + menuItem.getAllergen() + " " + menuItem.getRestaurantId());
        MenuItem item = new MenuItem();
        item.setName(menuItem.getName());
        item.setDescription(menuItem.getDescription());
        item.setAllergens(menuItem.getAllergen());
        item.setRestaurant(entityManager.getReference(Restaurant.class, menuItem.getRestaurantId()));
        System.out.println("Item: " + item + " " + item.getName() + " " + item.getDescription() + " " + item.getAllergens() + " " + item.getRestaurant());
        menuItemDAO.save(item);
    }

    public void addMenu(NewRestaurantMenuDTO menu) {
        RestaurantMenu newMenu = new RestaurantMenu();
        newMenu.setName(menu.getName());
        newMenu.setDescription(menu.getDescription());
        newMenu.setPrice(menu.getPrice());
        newMenu.setRestaurant(entityManager.getReference(Restaurant.class, menu.getRestaurantId()));
        menuDAO.save(newMenu);
    }

    public void deleteMenuItem(Long id) {
        menuItemDAO.deleteById(id);
    }

    public void deletePricedMenuItem(Long itemId, Long menuId) {
        pricedMenuItemDAO.deleteById(new MenuHasItemId(itemId, menuId));
    }

    public void deleteMenu(Long id) {
        menuDAO.deleteById(id);
    }


    
}
