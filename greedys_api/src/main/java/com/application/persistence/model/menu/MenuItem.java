package com.application.persistence.model.menu;

import java.util.List;

import com.application.persistence.model.restaurant.Restaurant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Represents an item on the menu.
 * 
 * <p>This entity is mapped to the "menu_item" table in the database.</p>
 * 
 * <p>Each MenuItem has the following attributes:</p>
 * <ul>
 *   <li><b>id</b>: The unique identifier for the menu item.</li>
 *   <li><b>name</b>: The name of the menu item.</li>
 *   <li><b>description</b>: A description of the menu item.</li>
 *   <li><b>ingredients</b>: A List of ingredients used in the menu item.</li>
 *   <li><b>allergens</b>: A List of allergens present in the menu item.</li>
 * </ul>
 * 
 * <p>The MenuItem class also contains an enumeration of possible allergens.</p>
 * 
 * <p>Relationships:</p>
 * <ul>
 *   <li><b>menusWithItem</b>: A many-to-many relationship with the MenuHasItem entity, indicating which menus contain this item.</li>
 * </ul>
 * 
 * <p>Annotations:</p>
 * <ul>
 *   <li><b>@Entity</b>: Specifies that this class is an entity and is mapped to a database table.</li>
 *   <li><b>@Table(name = "menu_item")</b>: Specifies the name of the database table to be used for mapping.</li>
 *   <li><b>@Id</b>: Specifies the primary key of the entity.</li>
 *   <li><b>@GeneratedValue(strategy = GenerationType.IDENTITY)</b>: Specifies the primary key generation strategy.</li>
 *   <li><b>@ManyToMany(mappedBy = "item")</b>: Specifies a many-to-many relationship with the MenuHasItem entity.</li>
 * </ul>
 */
@Entity
@Table(name = "menu_item")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;

    private List<String> ingredients;
    
    public enum Allergen {
        GLUTEN,
        CRUSTACEANS,
        EGGS,
        FISH,
        PEANUTS,
        SOYBEANS,
        MILK,
        NUTS,
        CELERY,
        MUSTARD,
        SESAME,
        SULPHUR_DIOXIDE,
        LUPIN,
        MOLLUSCS
    }
    private List<Allergen> allergens;

    @OneToMany(mappedBy = "item")
    private List<MenuHasItem> menusWithItem;

    @ManyToOne
    private Restaurant restaurant;


    // Getters and Setters
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

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public List<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<Allergen> allergens) {
        this.allergens = allergens;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }


}
