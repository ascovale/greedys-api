package com.application.agency.persistence.model.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import com.application.agency.persistence.model.Agency;
import com.application.common.persistence.model.user.AbstractUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * AgencyUser represents a user who works for a specific agency.
 * Similar to RUser but for agency operations.
 * Each AgencyUser works for ONE Agency (ManyToOne).
 * Each AgencyUser belongs to ONE AgencyUserHub (ManyToOne).
 */
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "agency_user")
@Getter
@Setter
public class AgencyUser extends AbstractUser {

    @ManyToOne(targetEntity = Agency.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "agency_user_has_role", 
        joinColumns = @JoinColumn(name = "agency_user_id"), 
        inverseJoinColumns = @JoinColumn(name = "agency_role_id")
    )
    @Builder.Default
    private List<AgencyRole> agencyRoles = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    private AgencyUserOptions options;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agency_user_hub_id")
    private AgencyUserHub agencyUserHub;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.VERIFY_TOKEN;

    @Column(name = "position")
    private String position; // Job title/position in agency

    @Column(name = "employee_id")
    private String employeeId; // Internal employee ID

    @Column(name = "department")
    private String department; // Department within agency

    @Column(name = "commission_rate")
    private Double commissionRate; // Commission rate for bookings

    @Column(name = "notes")
    private String notes; // Additional notes about this user

    public enum Status {
        VERIFY_TOKEN,
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

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
    public String getPassword() {
        // AgencyUser gets password from hub if available, otherwise from AbstractUser
        // For agency users, password should be managed through the hub system
        return super.getPassword();
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
        for (final AgencyRole role : agencyRoles) {
            privileges.add(role.getName());
            for (final AgencyPrivilege item : role.getPrivileges()) {
                privileges.add(item.getName());
            }
        }
        return privileges;
    }

    @Override
    public List<AgencyPrivilege> getPrivileges() {
        final List<AgencyPrivilege> privileges = new ArrayList<>();
        for (final AgencyRole role : agencyRoles) {
            privileges.addAll(role.getPrivileges());
        }
        return privileges;
    }

    @Override
    public List<AgencyRole> getRoles() {
        return agencyRoles;
    }

    @Override
    public String getUsername() {
        return super.getEmail();
    }

    /**
     * Get full name for display
     */
    public String getFullName() {
        return getName() + " " + (getSurname() != null ? getSurname() : "");
    }

    /**
     * Check if user has specific privilege
     */
    public boolean hasPrivilege(String privilegeName) {
        return getPrivilegesStrings().contains(privilegeName);
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String roleName) {
        return agencyRoles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
}