package com.application.persistence.model.restaurant.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.application.persistence.model.restaurant.Restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurant_user")
public class RestaurantUser {
    @Id
	@Column(unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
    @ManyToOne(targetEntity = Restaurant.class)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
    @ManyToMany
	@JoinTable(name = "user_restaurant_has_role", joinColumns = @JoinColumn(name = "restaurant_user_id"), inverseJoinColumns = @JoinColumn(name = "restaurant_role_id"))
	private Collection<RestaurantRole> roles;
    @OneToOne
    private RestaurantUserOptions options;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
    private String phoneNumber;
    @Column(length = 60)
	private String password;
    private String email;
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public enum Status {
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

	public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    private Integer toReadNotification = 0;

    public Integer getToReadNotification() {
        return toReadNotification;
    }

    public void setToReadNotification(Integer toReadNotification) {
        this.toReadNotification = toReadNotification;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public Long getId() {
        return id;
    }

    public RestaurantUserOptions getUserOptions() {
        return options;
    }

    public void setUserOptions(RestaurantUserOptions options) {
        this.options = options;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Collection<RestaurantRole> getRoles() {
        return roles;
    }

    public void setRoles(Collection<RestaurantRole> roles) {
        this.roles = roles;
    }

    public void addRole(RestaurantRole role) {
        this.roles.add(role);
    }

    public void removeRole(RestaurantRole role) {
        this.roles.remove(role);
    }

    public boolean hasRestaurantRole(String string) {
        
        for (RestaurantRole role : roles) {
            if (role.getName().equals(string)) {
                return true;
            }
        }
        return false;
    }

    public List<RestaurantPrivilege> getPrivileges() {
        List<RestaurantPrivilege> privileges = new ArrayList<>();
        for (RestaurantRole role : roles) {
            privileges.addAll(role.getPrivileges());
        }
        return privileges;
    }
}
