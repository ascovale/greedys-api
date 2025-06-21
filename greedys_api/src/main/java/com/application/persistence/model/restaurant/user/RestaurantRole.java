package com.application.persistence.model.restaurant.user;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurant_role")
public class RestaurantRole {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
	@ManyToMany
    private Collection<RUser> users;
    @ManyToMany
    @JoinTable(name = 	"restaurant_privilege_has_restaurant_role", 
    	joinColumns = @JoinColumn(name = "restaurant_role_id"), 
    	inverseJoinColumns = @JoinColumn(name = "restaurant_privilege_id")
    )	
    private Collection<RestaurantPrivilege> restaurantPrivileges;

    public RestaurantRole() {
        super();
    }

    public RestaurantRole(final String name) {
        super();
        this.name = name;
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

	public Collection<RUser> getUsers() {
		return users;
	}

	public void setUsers(Collection<RUser> users) {
		this.users = users;
	}

	public Collection<RestaurantPrivilege> getRestaurantPrivileges() {
		return restaurantPrivileges;
	}

	public void setRestaurantPrivileges(Collection<RestaurantPrivilege> restaurantPrivileges) {
		this.restaurantPrivileges = restaurantPrivileges;
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
        .append("[id=").append(id).append("]");
        return builder.toString();
    }
    public void setPrivileges(Collection<RestaurantPrivilege> privileges) {
		this.restaurantPrivileges = privileges;
	}

    public void addRestaurantPrivilege(RestaurantPrivilege rp) {
        if (restaurantPrivileges == null) {
            restaurantPrivileges = new ArrayList<>();
        }
        restaurantPrivileges.add(rp);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RestaurantRole role = (RestaurantRole) obj;
        if (!name.equals(role.name)) {
            return false;
        }
        return true;
    }
}