package com.application.persistence.model.restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.application.persistence.model.user.User;

@Entity
@Table(name = "restaurant_user")
public class RestaurantUser {
    @Id
	@Column(unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
    // private String nickname;
    @ManyToOne(targetEntity = Restaurant.class)
    private Restaurant restaurant;
    @OneToOne
    private User user;
    @OneToOne
    private RestaurantRole role;
    @OneToOne
    private RestaurantUserOptions options;

    private Boolean accepted = false;
    private Boolean rejected = false;
    private Boolean blocked = false;
    private Boolean deleted = false;

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setRole(RestaurantRole role) {
        this.role = role;
    }

    public RestaurantRole getRoles() {
        return role;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setRejected(Boolean rejected) {
        this.rejected = rejected;
    }

    public Boolean getRejected() {
        return rejected;
    }

    public Long getId() {
        return id;
    }

    public RestaurantUserOptions getUserOptions() {
        return options;
    }
    
}
