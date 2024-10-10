package com.application.persistence.model.menu;

import java.io.Serializable;

import java.util.Objects;

public class MenuHasItemId implements Serializable {

    private Long menu;
    private Long item;

    public MenuHasItemId(Long menu, Long item) {
        this.menu = menu;
        this.item = item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuHasItemId)) return false;
        MenuHasItemId that = (MenuHasItemId) o;
        return menu.equals(that.menu) && item.equals(that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menu, item);
    }
    
}
