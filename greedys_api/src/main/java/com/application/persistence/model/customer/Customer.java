package com.application.persistence.model.customer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.user.AbstractUser;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer extends AbstractUser {
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
    private Set<Reservation> reservations = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "customer_has_role", 
        joinColumns = @JoinColumn(name = "customer_id"), 
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles = new ArrayList<>();
    private Integer toReadNotification = 0;
    @OneToOne
    private CustomerOptions customerOptions;
    @ManyToMany
    @JoinTable(name = "user_allergy", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "allergy_id"))
    private List<Allergy> allergies;
    private String dateOfBirth;

    public enum Status {
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED,
        VERIFY_TOKEN,
        AUTO_DELETE
    }
    private Status status = Status.VERIFY_TOKEN;

    public CustomerOptions getCustomerOptions() {
        return customerOptions;
    }
    public void setCustomerOptions(CustomerOptions options) {
        this.customerOptions = options;
    }
    public void setToReadNotification(Integer toReadNotification) {
        this.toReadNotification = toReadNotification;
    }
    public List<Allergy> getAllergies() {
        return allergies;
    }
    public void setAllergies(List<Allergy> allergies) {
        this.allergies = allergies;
    }
    public Set<Reservation> getReservations() {
        return reservations;
    }
    public void setReservations(Set<Reservation> reservations) {
        this.reservations = reservations;
    }
    @Override
    public boolean isEnabled() {
        return status == Status.ENABLED ;    
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    @Override
    public List<Role> getRoles() {
        return roles;
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
    public Integer getToReadNotification() {
        return toReadNotification;
    }
    public void addRole(Role role) {
        if (roles == null) {
            roles = new ArrayList<>();
        }
        if (!roles.contains(role)) {
            roles.add(role);
        }
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
	public String getUsername() {
		return email;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSurname() {
		return surname;
	}
	@Override
	public List<Privilege> getPrivileges() {
		final List<Privilege> privileges = new ArrayList<>();
		for (final Role role : roles) {
			privileges.addAll(role.getPrivileges());
		}
		return privileges;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}
    public String getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}