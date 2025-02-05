package com.application.persistence.model.user;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantUser;

@Entity
@Table(name = "user")
public class User implements UserDetails {
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
	private String numero_di_telefono;
	@OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
	private Collection<RestaurantUser> restaurants;
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
	private Set<Reservation> reservations;
	@ManyToMany
	@JoinTable(name = "user_has_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Collection<Role> roles;
	@OneToOne
	private RestaurantUser restaurantUser;
	private Boolean blooked = false;
	private Boolean deleted = false;
	private Integer toReadNotification = 0;
	
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

	public RestaurantUser getRestaurantUser() {
		return restaurantUser;
	}

	public void setRestaurantUser(RestaurantUser restaurantUser) {
		this.restaurantUser = restaurantUser;
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

	public Collection<Restaurant> getRestaurants() {

		return restaurants.stream().map(RestaurantUser::getRestaurant)
				.collect(Collectors.toSet());
	}

	public Collection<Role> getRestaurantRoles() {
		return roles;
	}

	public void setRestaurantRoles(Collection<Role> restaurantRoles) {
		this.roles = restaurantRoles;
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

	public String getNumero_di_telefono() {
		return numero_di_telefono;
	}

	public void setNumero_di_telefono(String numero_di_telefono) {
		this.numero_di_telefono = numero_di_telefono;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getAuthorities'");
	}

	@Override
	public String getUsername() {
		return email;
	}

	public Integer getToReadNotification() {
		return toReadNotification;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO: Metodo richiesto quando ho aggiornato le dipendenze bisogna verificare
		// come è necessario implementarlo
		return true; // Implementa la logica appropriata per il tuo caso d'uso
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO: Metodo richiesto quando ho aggiornato le dipendenze bisogna verificare
		// come è necessario implementarlo
		// Implementa la logica per verificare se l'account non è scaduto
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO: Metodo richiesto quando ho aggiornato le dipendenze bisogna verificare
		// come è necessario implementarlo
		// Implementa la logica per verificare se l'account non è bloccato
		return true;
	}

}