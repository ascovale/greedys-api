package com.application.persistence.model.restaurant.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
public class RestaurantUser implements UserDetails {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne(targetEntity = Restaurant.class)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
    @ManyToMany
    @JoinTable(name = "restaurant_user_has_role", joinColumns = @JoinColumn(name = "restaurant_user_id"), inverseJoinColumns = @JoinColumn(name = "restaurant_role_id"))
    private Collection<RestaurantRole> restaurantRoles;
    @OneToOne
    private RestaurantUserOptions options;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.ENABLED;
    private String phoneNumber;
    @Column(length = 60)
    private String password;
    private String email;
    private boolean accepted;
    private String firstName;
    private String lastName;

    public enum Status {
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    @Override
    public boolean isEnabled() {
        return status == Status.ENABLED && restaurant != null && restaurant.getStatus() == Restaurant.Status.ENABLED;
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

    public Collection<RestaurantRole> getRestaurantRoles() {
        return restaurantRoles;
    }

    public void setRoles(Collection<RestaurantRole> restaurantRoles) {
        this.restaurantRoles = restaurantRoles;
    }

    public void addRestaurantRole(RestaurantRole restaurantRoles) {
        if (this.restaurantRoles == null) {
            this.restaurantRoles = new ArrayList<>();
        }
        this.restaurantRoles.add(restaurantRoles);
    }

    public void removeRole(RestaurantRole role) {
        this.restaurantRoles.remove(role);
    }

    public boolean hasRestaurantRole(String string) {

        for (RestaurantRole restaurantRole : restaurantRoles) {
            if (restaurantRole.getName().equals(string)) {
                return true;
            }
        }
        return false;
    }

    public List<RestaurantPrivilege> getPrivileges() {
        List<RestaurantPrivilege> privileges = new ArrayList<>();
        for (RestaurantRole restaurantRole : restaurantRoles) {
            privileges.addAll(restaurantRole.getRestaurantPrivileges());
        }
        return privileges;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getRestaurantPrivileges());
    }

    private final List<String> getRestaurantPrivileges() {
        final List<String> privileges = new ArrayList<String>();

        for (final RestaurantRole role : restaurantRoles) {
            privileges.add(role.getName());
            for (final RestaurantPrivilege item : role.getRestaurantPrivileges()) {
                privileges.add(item.getName());
            }
        }

        return privileges;
    }

    private final List<GrantedAuthority> getGrantedAuthorities(final List<String> privileges) {
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status == Status.ENABLED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != Status.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        //TODO in futuro da cambiare se le credenziali scadono
        return true;
    }

}
