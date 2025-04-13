package com.application.persistence.model.menu;

import java.util.List;

import com.application.persistence.model.restaurant.Restaurant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "plate")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @ManyToMany
    @JoinTable(
        name = "dish_photo",
        joinColumns = @JoinColumn(name = "plate_id"),
        inverseJoinColumns = @JoinColumn(name = "photo_id")
    )
    private List<DishPhoto> photos;
    private List<String> photoLinks;
    @ManyToMany
    @JoinTable(
        name = "dish_category",
        joinColumns = @JoinColumn(name = "dish_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<DishCategory> categories;
    @ManyToMany
    @JoinTable(
        name = "dish_ingredient",
        joinColumns = @JoinColumn(name = "dish_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private List<Ingredient> ingredients;
    @OneToMany(mappedBy = "dish")
    private List<MenuDish> menuDishes;
    @ManyToMany
    @JoinTable(
        name = "dish_menu",
        joinColumns = @JoinColumn(name = "dish_id"),
        inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    private List<Menu> menus;
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToMany
    @JoinTable(
        name = "dish_allergen",
        joinColumns = @JoinColumn(name = "dish_id"),
        inverseJoinColumns = @JoinColumn(name = "allergen_id")
    )
    private List<Allergen> allergens;

    public List<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<Allergen> allergens) {
        this.allergens = allergens;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DishPhoto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<DishPhoto> photos) {
        this.photos = photos;
    }

    public List<String> getPhotoLinks() {
        return photoLinks;
    }

    public void setPhotoLinks(List<String> photoLinks) {
        this.photoLinks = photoLinks;
    }

    public List<DishCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<DishCategory> categories) {
        this.categories = categories;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<MenuDish> getMenuDishes() {
        return menuDishes;
    }

    public void setMenuDishes(List<MenuDish> menuDishes) {
        this.menuDishes = menuDishes;
    }

    public List<Menu> getMenus() {
        return menus;
    }

    public void setMenus(List<Menu> menus) {
        this.menus = menus;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }
}