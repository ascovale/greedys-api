package com.application.persistence.model.menu;

import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;



@Entity
@IdClass(MenuHasItemId.class)
public class MenuHasItem {

    @Id
    @ManyToOne
    @JoinColumn(name = "item_id")
    private MenuItem item;

    @Id
    @ManyToOne
    @JoinColumn(name = "menu_id")
    private RestaurantMenu menu;

    private float price;

    public MenuHasItem(MenuItem item, RestaurantMenu menu) {
        this.item = item;
        this.menu = menu;
    }

    public MenuItem getItemId() {
        return item;
    }

    public RestaurantMenu getMenuId() {
        return menu;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
    
}
