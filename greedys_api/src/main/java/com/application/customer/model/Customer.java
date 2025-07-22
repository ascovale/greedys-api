package com.application.customer.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.user.AbstractUser;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "customer")
public class Customer extends AbstractUser {

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
    @Builder.Default
    private Set<Reservation> reservations = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "customer_has_role", 
        joinColumns = @JoinColumn(name = "customer_id"), 
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "customer_options_id")
    private CustomerOptions customerOptions;

    @ManyToMany
    @JoinTable(name = "user_allergy", 
        joinColumns = @JoinColumn(name = "user_id"), 
        inverseJoinColumns = @JoinColumn(name = "allergy_id"))
    private List<Allergy> allergies;

    @Builder.Default
    private Status status = Status.VERIFY_TOKEN;

    public enum Status {
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED,
        VERIFY_TOKEN,
        AUTO_DELETE
    }

    private Date dateOfBirth;

    @Override
    public boolean isEnabled() {
        return status == Status.ENABLED;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != Status.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == Status.ENABLED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getPrivilegesStrings());
    }

    @Override
    protected List<GrantedAuthority> getGrantedAuthorities(List<String> privileges) {
        return super.getGrantedAuthorities(privileges);
    }

    @Override
    public List<String> getPrivilegesStrings() {
        final List<String> privileges = new ArrayList<>();
        for (final Role role : roles) {
            privileges.add(role.getName());
            for (final Privilege item : role.getPrivileges()) {
                privileges.add(item.getName());
            }
        }
        return privileges;
    }

    @Override
    public List<Privilege> getPrivileges() {
        final List<Privilege> privileges = new ArrayList<>();
        for (final Role role : roles) {
            privileges.addAll(role.getPrivileges());
        }
        return privileges;
    }

    @Override
    public String getUsername() {
        return super.getEmail();
    }
}