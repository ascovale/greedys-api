package com.application.persistence.model.reservation;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.application.persistence.model.menu.Menu;
import com.application.persistence.model.restaurant.Restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "service")
public class Service {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;

	@ManyToOne(fetch = FetchType.EAGER) // qui potrebbe essere utile condividere il servizio tra più ristoranti in
										// alcuni casi
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "service_service_type", joinColumns = @JoinColumn(name = "service_id"), inverseJoinColumns = @JoinColumn(name = "type_id"))
	private Set<ServiceType> serviceTypes = new HashSet<>();

	@OneToMany(mappedBy = "service", fetch = FetchType.EAGER)
	private Set<Slot> slots;

	@Column(name = "info")
	private String info;

	private Boolean deleted = false;


	private LocalDate validFrom;
	private LocalDate validTo;

	private boolean active = true;

	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<Menu> getMenus() {
		return menus;
	}

	public void setMenus(List<Menu> menus) {
		this.menus = menus;
	}

	// colore del servizio
	private String color;
	@ManyToMany
	@JoinTable(
		name = "menu_service",
		joinColumns = @JoinColumn(name = "service_id"),
		inverseJoinColumns = @JoinColumn(name = "menu_id")
	)
	private List<Menu> menus;
	
	public boolean isActive() {
		return active;
	}
	
	public boolean isActiveNow() {
		LocalDate today = LocalDate.now();
		return isActiveInDate(today);
	}

	public boolean isActiveInDate(LocalDate date) {
		if (validFrom != null && validTo != null) {
			if (date.isBefore(validFrom) || date.isAfter(validTo)) {
				return false;
			}
		}
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public LocalDate getValidTo() {
		return validTo;
	}

	public void setValidTo(LocalDate validTo) {
		this.validTo = validTo;
	}

	public Set<ServiceType> getServiceType() {
		return serviceTypes;
	}

	
	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}
	
	public void setServiceTypes(Set<ServiceType> serviceType) {
		this.serviceTypes = serviceType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}

	public Set<Slot> getSlots() {
		return slots;
	}

	public void setSlots(Set<Slot> slots) {
		this.slots = slots;
	}

	public String getInfo() {
		return this.info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

    public void addServiceType(ServiceType pranzoType) {
		if (serviceTypes == null) {
			serviceTypes = new HashSet<>();
		}
		serviceTypes.add(pranzoType);
    }

}
