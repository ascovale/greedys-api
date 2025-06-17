package com.application.persistence.model.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin")
public class Admin implements UserDetails {
	@Id
	@Column(unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private String surname;
	private String email;
	@Column(length = 60)
	private String password;
	private String phoneNumber;
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

	public List<AdminRole> getAdminRoles() {
		return adminRoles;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setAdminRoles(List<AdminRole> adminRoles) {
		this.adminRoles = adminRoles;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((email == null) ? 0 : email.hashCode());
		return result;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumeber) {
		this.phoneNumber = phoneNumeber;
	}

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getRestaurantPrivileges());
    }

    private final List<String> getRestaurantPrivileges() {
        final List<String> privileges = new ArrayList<String>();

        for (final AdminRole role : adminRoles) {
            privileges.add(role.getName());
            for (final AdminPrivilege item : role.getAdminPrivileges()) {
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

	public Integer getToReadNotification() {
		return toReadNotification;
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
		//TODO in futuro da cambiare se le credenziali scadono
		return true;
	}

	public void addAdminRole(AdminRole adminRole) {
		if (this.adminRoles == null) {
			this.adminRoles = new ArrayList<>();
		}
		this.adminRoles.add(adminRole);
	}
	

}