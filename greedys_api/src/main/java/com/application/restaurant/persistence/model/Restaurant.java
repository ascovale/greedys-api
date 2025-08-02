package com.application.restaurant.persistence.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.application.common.persistence.model.Image;
import com.application.common.persistence.model.reservation.Service;
import com.application.restaurant.persistence.model.menu.Dish;
import com.application.restaurant.persistence.model.user.RUser;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedQuery(name = "Restaurant.findBySearchTermNamed", query = "SELECT t FROM Restaurant t WHERE "
		+ "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))" + "ORDER BY t.name ASC")
@Entity
@Table(name = "restaurant")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	@Column(name = "date_creation")
	private LocalDate creationDate;
	private String email;
	@Column(name = "phone_number")
	private String phoneNumber;
	private String address;
	@Column(name = "postal_code")
	private String postCode;
	@Column(name = "city")
	private String city;
	@Column(name = "state_province")
	private String stateProvince;
	@Column(name = "country")
	private String country;
	@Column(name = "latitude")
	private Double latitude;
	@Column(name = "longitude")
	private Double longitude;
	@Column(name = "vat_number", length = 20)
	private String vatNumber; // International VAT number (e.g., IT12345678901)
	@Column(name = "description")
	private String description;
	private String placeId; // Google Place ID
	private String website; // Website URL
	private String priceLevel;
	// TODO cambiare il fetch in LAZY
	// bisogna fare in modo che la pageable prende anche le foto basta fare il join
	// fetch ma è solo in jpql
	// quindi da vedere come fare con una query normale o come scrivere in jpql
	// si potrebbe fare un JOIN FETCH o fetch join con RestaurantImage ir nella
	// clausola from
	@OneToMany(fetch = FetchType.EAGER, mappedBy = "restaurant")
	private List<Service> services;
	@OneToMany(mappedBy = "restaurant")
	private Collection<RUser> RUsers;
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "restaurant")
	// @JsonManagedReference
	private List<Image> restaurantImages;
	@Builder.Default
	@Column(name = "no_show_time_limit", columnDefinition = "integer default 15")
	private Integer noShowTimeLimit = 15;
	@ManyToMany
	@JoinTable(name = "restaurant_has_restaurant_type", joinColumns = @JoinColumn(name = "restaurant_id"), inverseJoinColumns = @JoinColumn(name = "restaurant_type_id"))
	private List<RestaurantCategory> restaurantTypes;
	@Builder.Default
	private Boolean waNotification = false; // manda un messaggio whatsapp
	@Builder.Default
	private Boolean telegramNotification = false;
	public enum Status {
		ENABLED,
		DISABLED,
		DELETED,
		CLOSED,
		TEMPORARILY_CLOSED
	}

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private Status status;
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "restaurant")
	private List<Dish> dishes;

	@Column(name = "wa_notification_time_advance", columnDefinition = "integer default 30")
	@Builder.Default
	private Integer messageNotificationTimeAdvance = 30;

	// Phone verification fields
	@Column(name = "phone_verified", columnDefinition = "boolean default false")
	@Builder.Default
	private Boolean phoneVerified = false;

	@Column(name = "phone_verified_at")
	private java.time.LocalDateTime phoneVerifiedAt;

	// Metodi personalizzati con logica di business
	public List<Dish> getDishes() {
		if (dishes == null) {
			dishes = new ArrayList<>();
		}
		return dishes;
	}

	public List<RestaurantCategory> getRestaurantTypes() {
		if (restaurantTypes == null) {
			restaurantTypes = new ArrayList<>();
		}
		return restaurantTypes;
	}

	public void setRestaurantTypes(List<RestaurantCategory> restaurantTypes) {
		if (this.restaurantTypes == null) {
			this.restaurantTypes = new ArrayList<>();
		}
		this.restaurantTypes = restaurantTypes;
	}

	/**
	 * Legacy getter for backward compatibility
	 * @deprecated Use getVatNumber() instead
	 */
	@Deprecated
	public String getPI() {
		return this.vatNumber;
	}

	/**
	 * Legacy setter for backward compatibility
	 * @deprecated Use setVatNumber() instead
	 */
	@Deprecated
	public void setPI(String pI) {
		this.vatNumber = pI;
	}

}
