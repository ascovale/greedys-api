package com.application.persistence.model.restaurant.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.user.BaseUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
public class RUser extends BaseUser {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne(targetEntity = Restaurant.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "restaurant_user_has_role", joinColumns = @JoinColumn(name = "restaurant_user_id"), inverseJoinColumns = @JoinColumn(name = "restaurant_role_id"))
    private List<RestaurantRole> restaurantRoles = new ArrayList<>();
    @OneToOne(fetch = FetchType.LAZY)
    private RUserOptions options;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.VERIFY_TOKEN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_user_hub_id")
    private RUserHub RUserHub;

    public RUserHub getRUserHub() {
        return RUserHub;
    }

    public void setRUserHub(RUserHub RUserHub) {
        this.RUserHub = RUserHub;
    }

    private boolean accepted;

    public enum Status {
        VERIFY_TOKEN,
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public RUserOptions getUserOptions() {
        return options;
    }

    public void setUserOptions(RUserOptions options) {
        this.options = options;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public List<RestaurantRole> getRoles() {
        return restaurantRoles;
    }

    public void setRoles(List<RestaurantRole> restaurantRoles) {
        this.restaurantRoles = restaurantRoles;
    }

    public void addRestaurantRole(RestaurantRole restaurantRole) {
        if (this.restaurantRoles == null) {
            this.restaurantRoles = new ArrayList<>();
        }
        this.restaurantRoles.add(restaurantRole);
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
            privileges.addAll(restaurantRole.getPrivileges());
        }
        return privileges;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getPrivilegesStrings());
    }

    public final List<String> getPrivilegesStrings() {
        final List<String> privileges = new ArrayList<String>();

        for (final RestaurantRole role : restaurantRoles) {
            privileges.add(role.getName());
            for (final RestaurantPrivilege item : role.getPrivileges()) {
                privileges.add(item.getName());
            }
        }

        return privileges;
    }

    protected final List<GrantedAuthority> getGrantedAuthorities(final List<String> privileges) {
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (final String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        if (RUserHub != null && restaurant != null) {
            return RUserHub.getEmail() + ":" + restaurant.getId();
        }
        return null;
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
        return true;
    }

    @Override
    public String getPassword() {
        return RUserHub != null ? RUserHub.getPassword() : null;
    }

    public String getEmail() {
        return RUserHub != null ? RUserHub.getEmail() : null;
    }

    @Override
    public String getPhoneNumber() {
        return RUserHub != null ? RUserHub.getPhoneNumber() : null;
    }

    @Override
    public String getName() {
        return RUserHub != null ? RUserHub.getFirstName() : null;
    }

    @Override
    public String getSurname() {
        return RUserHub != null ? RUserHub.getLastName() : null;
    }



}
