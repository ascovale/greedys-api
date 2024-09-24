package com.application.persistence.model.restaurant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "logo_restaurant")
public class LogoRestaurant {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String path;
	@OneToOne
	@JoinColumn(name = "restaurant_id", referencedColumnName = "id")
	private Restaurant restaurant;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Restaurant getRestaurant() {
		return restaurant;
	}
	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}
}
