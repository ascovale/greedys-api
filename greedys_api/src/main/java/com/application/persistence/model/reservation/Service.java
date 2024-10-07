package com.application.persistence.model.reservation;

import java.util.Set;
import java.util.HashSet;
import java.util.Date;

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

import com.application.persistence.model.restaurant.Restaurant;



@Entity
@Table(name = "service")
public class Service {
	
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	private String name;

	@ManyToOne(fetch = FetchType.EAGER) // qui potrebbe essere utile condividere il servizio tra più ristoranti in alcuni casi
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "service_service_type",
		joinColumns = @JoinColumn(name = "service_id"),
		inverseJoinColumns = @JoinColumn(name = "type_id")
	)
	private Set<ServiceType> serviceTypes = new HashSet<>();

	@OneToMany(mappedBy = "service", fetch = FetchType.EAGER)
	private Set<Slot> slots;

	@Column(name = "info")
	private String info;

	private Date validFrom;
	private Date validTo;

	private boolean active = true;

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}


	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}
	
	public Set<ServiceType> getServiceType() {
		return serviceTypes;
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
        this.slots=slots;
    }

	public String getInfo() {
		return this.info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
	
}
