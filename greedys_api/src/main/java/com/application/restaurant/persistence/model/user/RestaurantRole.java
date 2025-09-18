package com.application.restaurant.persistence.model.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.application.common.persistence.model.user.BaseRole;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurant_role")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRole implements BaseRole<RestaurantPrivilege> {

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
    private List<RestaurantPrivilege> restaurantPrivileges;

    // Costruttore personalizzato per mantenere la logica esistente
    public RestaurantRole(final String name) {
        super();
        this.name = name;
    }

    public void addRestaurantPrivilege(RestaurantPrivilege rp) {
        if (restaurantPrivileges == null) {
            restaurantPrivileges = new ArrayList<>();
        }
        restaurantPrivileges.add(rp);
    }

    // Metodi richiesti dall'interfaccia BaseRole
    public List<RestaurantPrivilege> getPrivileges() {
        return restaurantPrivileges;
    }

    @Override
    public void setPrivileges(List<RestaurantPrivilege> privileges) {
        this.restaurantPrivileges = privileges;
    }

    @Override
    public void addPrivilege(RestaurantPrivilege privilege) {
        if (restaurantPrivileges == null) {
            restaurantPrivileges = new ArrayList<>();
        }
        restaurantPrivileges.add(privilege);
    }

}
