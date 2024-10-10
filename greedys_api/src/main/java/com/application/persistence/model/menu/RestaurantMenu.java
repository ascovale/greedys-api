
package com.application.persistence.model.menu;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Collection;

import com.application.persistence.model.restaurant.Restaurant;


/**
 * Entity representing a restaurant menu.
 * 
 * <p>This class is mapped to a database table using JPA annotations. It contains
 * information about a restaurant menu, including its ID, name, description, price,
 * and a collection of menu items.</p>
 * 
 * <p>Annotations:</p>
 * <ul>
 *   <li>{@code @Entity} - Specifies that the class is an entity and is mapped to a database table.</li>
 *   <li>{@code @Id} - Specifies the primary key of an entity.</li>
 *   <li>{@code @GeneratedValue} - Provides for the specification of generation strategies for the values of primary keys.</li>
 * </ul>
 * 
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code id} - The unique identifier for the restaurant menu.</li>
 *   <li>{@code name} - The name of the restaurant menu.</li>
 *   <li>{@code description} - A brief description of the restaurant menu.</li>
 *   <li>{@code price} - The price of the restaurant menu.</li>
 *   <li>{@code items} - A collection of {@code MenuItem} objects that are part of the restaurant menu.</li>
 * </ul>
 * 
 * <p>Methods:</p>
 * <ul>
 *   <li>{@code getId()} - Returns the ID of the restaurant menu.</li>
 *   <li>{@code setId(Long id)} - Sets the ID of the restaurant menu.</li>
 *   <li>{@code getName()} - Returns the name of the restaurant menu.</li>
 *   <li>{@code setName(String name)} - Sets the name of the restaurant menu.</li>
 *   <li>{@code getDescription()} - Returns the description of the restaurant menu.</li>
 *   <li>{@code setDescription(String description)} - Sets the description of the restaurant menu.</li>
 *   <li>{@code getPrice()} - Returns the price of the restaurant menu.</li>
 *   <li>{@code setPrice(Float price)} - Sets the price of the restaurant menu.</li>
 *   <li>{@code getItems()} - Returns the collection of menu items that are part of the restaurant menu.</li>
 *   <li>{@code setItems(Collection<MenuItem> items)} - Sets the collection of menu items that are part of the restaurant menu.</li>
 * </ul>
 */
@Entity
@Table(name = "restaurant_menu")
public class RestaurantMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;

    private Float price;

    @OneToMany(mappedBy = "menu")
    private Collection<MenuHasItem> items;

    @ManyToOne
    private Restaurant restaurant;

    // Getters and setters
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


    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }
}
