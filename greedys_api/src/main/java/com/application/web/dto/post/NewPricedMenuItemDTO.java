package com.application.web.dto.post;

public class NewPricedMenuItemDTO {

    private Long menuId;
    private Long itemId;
    private float price;
    private Long restaurantId;

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public Long getItemId() {
        return itemId;
    }

    public float getPrice() {
        return price;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
    
}
