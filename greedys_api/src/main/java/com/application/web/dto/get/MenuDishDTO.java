package com.application.web.dto.get;

import com.application.persistence.model.menu.Dish;
import com.application.persistence.model.menu.MenuDish;

/**
 * PricedMenuItemDTO is a Data Transfer Object (DTO) that represents a menu item along with its price.
 * It contains a MenuItemDTO object and a price value.
 * 
 * <p>This class provides methods to retrieve the menu item and its price.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * MenuItem menuItem = new MenuItem(...);
 * float price = 9.99f;
 * PricedMenuItemDTO pricedMenuItemDTO = new PricedMenuItemDTO(menuItem, price);
 * }
 * </pre>
 * 
 * @see DishDTO
 */
public class MenuDishDTO {

    private DishDTO dishDTO;
    private Double price;
    
    public MenuDishDTO(MenuDish menuDish) {
        this.dishDTO = new DishDTO(menuDish.getDish());
        this.price = menuDish.getPrice();  
    }

    public MenuDishDTO(Dish dish, Double price) {
        this.dishDTO = new DishDTO(dish);
        this.price = price;
    }

    public DishDTO getItem() {
        return dishDTO;
    }

    public Double getPrice() {
        return price;
    }
    
}
