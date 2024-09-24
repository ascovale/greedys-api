package com.application.persistence.model.reservation;

import java.time.LocalTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.application.persistence.model.restaurant.Restaurant;

@Entity
@Table(name = "service")
public class Service {
	public static enum Type {NORMAL,SPECIAL};
		
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	Long id;
	String name;

	@ManyToOne(fetch = FetchType.EAGER) // qui potrebbe essere utile condividere il servizio tra più ristoranti in alcuni casi
    @JoinColumn(name = "restaurant_id")
	Restaurant restaurant;


	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id")
	ServiceType serviceType;

	@OneToMany(mappedBy = "service", fetch = FetchType.EAGER)
	Set<Slot> slots;

	@Column(name = "info")
	String info;
	
	public ServiceType getServiceType() {
		return serviceType;
	}

	public void setServiceType(ServiceType serviceType) {
		this.serviceType = serviceType;
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
