package com.application.admin.persistence.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import com.application.common.persistence.model.user.AbstractUser;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "admin")
public class Admin extends AbstractUser {
    @ManyToMany
    @JoinTable(name = "admin_has_role", 
        joinColumns = @JoinColumn(name = "admin_id"), 
        inverseJoinColumns = @JoinColumn(name = "admin_role_id"))
    @Builder.Default
    private List<AdminRole> adminRoles = new ArrayList<>();

    public enum Status {
        VERIFY_TOKEN,
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Status status = Status.VERIFY_TOKEN;

    public void addAdminRole(AdminRole adminRole) {
        if (this.adminRoles == null) {
            this.adminRoles = new ArrayList<>();
        }
        this.adminRoles.add(adminRole);
    }
    @Override
    public List<AdminRole> getRoles() {
        return adminRoles;
    }
    public List<String> getPrivalegesStrings() {
        final List<String> privileges = new ArrayList<>();
        for (final AdminRole role : adminRoles) {
            privileges.add(role.getName());
            for (final AdminPrivilege item : role.getPrivileges()) {
                privileges.add(item.getName());
            }
        }
        return privileges;
    }

    @Override
    protected List<GrantedAuthority> getGrantedAuthorities(List<String> privileges) {
        return super.getGrantedAuthorities(privileges);
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getPrivilegesStrings());
    }

	@Override
	public List<AdminPrivilege> getPrivileges() {
		final List<AdminPrivilege> privileges = new ArrayList<>();
		for (final AdminRole role : adminRoles) {
			privileges.addAll(role.getPrivileges());
		}
		return privileges;
	}
	@Override
	public List<String> getPrivilegesStrings() {
		final List<String> privileges = new ArrayList<>();
		for (final AdminRole role : adminRoles) {
			privileges.add(role.getName());
			for (final AdminPrivilege privilege : role.getPrivileges()) {
				privileges.add(privilege.getName());
			}
		}
		return privileges;
	}

}
