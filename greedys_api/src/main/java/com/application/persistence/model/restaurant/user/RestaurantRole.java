package com.application.persistence.model.restaurant.user;

import java.util.ArrayList;
import java.util.Collection;

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
@Table(name = "restaurant_role")
public class RestaurantRole {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    @ManyToOne
    private Restaurant restaurant;
	@ManyToMany
    private Collection<RestaurantUser> users;
    @ManyToMany(mappedBy = "roles")
    @JoinTable(name = 	"restaurant_privilege_has_restaurant_role", 
    	joinColumns = @JoinColumn(name = "restaurant_role_id"), 
    	inverseJoinColumns = @JoinColumn(name = "restaurant_privilege_id")
    )	
    private Collection<RestaurantPrivilege> privileges;

    public RestaurantRole() {
        super();
    }

    public RestaurantRole(final String name) {
        super();
        this.name = name;
    }

    //

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(final Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

	public Collection<RestaurantUser> getUsers() {
		return users;
	}

	public void setUsers(Collection<RestaurantUser> users) {
		this.users = users;
	}

	public Collection<RestaurantPrivilege> getPrivileges() {
		return privileges;
	}

	public void setRestaurantPrivileges(Collection<RestaurantPrivilege> privileges) {
		this.privileges = privileges;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Role [name=").append(name).append("] ")
        .append("Restaurant [id=").append(restaurant.getId()).append("] ")
        .append("[id=").append(id).append("]");
        return builder.toString();
    }
    public void setPrivileges(Collection<RestaurantPrivilege> privileges) {
		this.privileges = privileges;
	}

    public void addPrivilege(RestaurantPrivilege rp) {
        if (privileges == null) {
            privileges = new ArrayList<>();
        }
        privileges.add(rp);
    }
}