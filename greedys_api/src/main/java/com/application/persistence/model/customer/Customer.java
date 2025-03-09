package com.application.persistence.model.customer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.application.persistence.model.reservation.Reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer implements UserDetails {
	@Id
	@Column(unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private String surname;
	private String email;
	@Column(length = 60)
	private String password;
	private boolean enabled;
	private String phoneNumber;
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
	private Set<Reservation> reservations;
	@ManyToMany
	@JoinTable(name = "customer_has_role", joinColumns = @JoinColumn(name = "customer_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Collection<Role> roles;
	private Boolean blooked = false;
	private Boolean deleted = false;
	private Integer toReadNotification = 0;
	@OneToOne
    private CustomerOptions customerOptions;

	public enum Status {
        BLOCKED,
        DELETED,
        ENABLED,
        DISABLED
    }

	private Status status;

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public CustomerOptions getCustomerOptions() {
		return customerOptions;
	}

	public void setCustomerOptions(CustomerOptions options) {
		this.customerOptions = options;
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

	public Boolean getBlooked() {
		return blooked;
	}

	public void setIsBlooked(Boolean blooked) {
		this.blooked = blooked;
	}

	@ManyToMany
	@JoinTable(name = "user_allergy", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "allergy_id"))
	private List<Allergy> allergies;

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
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

	public Collection<Role> getRoles() {
		return roles;
	}

	public void setRoles(Collection<Role> roles) {
		this.roles = roles;
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
	public Collection<? extends GrantedAuthority> getAuthorities() {
        return getGrantedAuthorities(getPrivileges());
    }

    private final List<String> getPrivileges() {
        final List<String> privileges = new ArrayList<String>();

        for (final Role role : roles) {
            privileges.add(role.getName());
            for (final Privilege item : role.getPrivileges()) {
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
        return status != Status.BLOCKED;
    }

	@Override
	public boolean isCredentialsNonExpired() {
		//TODO in futuro da cambiare se le credenziali scadono

		return true;
	}

}