package com.application.web.dto.get;

import com.application.persistence.model.menu.MenuItem;

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
 * @see MenuItemDTO
 */
public class PricedMenuItemDTO {

    private MenuItemDTO item;
    private float price;

    public PricedMenuItemDTO(MenuItem menuItem, float price) {
        this.item = new MenuItemDTO(menuItem);
        this.price = price;
    }

    public MenuItemDTO getItem() {
        return item;
    }

    public float getPrice() {
        return price;
    }
    
}
