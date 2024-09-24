package com.application.persistence.model.restaurant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "image_restaurant")
public class RestaurantImage {
	
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
	Restaurant restaurant;

	public RestaurantImage(){};

	public RestaurantImage(String name) {
		this.name = name;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Image [id=" + id + ", name=" + name + "]";
	}

}
