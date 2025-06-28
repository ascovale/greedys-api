package com.application.persistence.model.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import com.application.persistence.model.user.AbstractUser;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin")
public class Admin extends AbstractUser {
    @ManyToMany
    @JoinTable(name = "admin_has_role", 
        joinColumns = @JoinColumn(name = "admin_id"), 
        inverseJoinColumns = @JoinColumn(name = "admin_role_id"))
    private List<AdminRole> adminRoles = new ArrayList<>();
    private Boolean blocked = false;
    private Boolean deleted = false;
    private Integer toReadNotification = 0;

    public enum Status {
        VERIFY_TOKEN,
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }
    private Status status = Status.VERIFY_TOKEN;

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public void setToReadNotification(Integer toReadNotification) {
        this.toReadNotification = toReadNotification;
    }
    public Boolean getDeleted() {
        return deleted;
    }
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
    public Boolean getBlocked() {
        return blocked;
    }
    public void setIsBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
    @Override
    public boolean isEnabled() {
        return status == Status.ENABLED ;    
    }
    public void setAdminRoles(List<AdminRole> adminRoles) {
        this.adminRoles = adminRoles;
    }
    public Integer getToReadNotification() {
        return toReadNotification;
    }
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
    public String getUsername() {
        return email;
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