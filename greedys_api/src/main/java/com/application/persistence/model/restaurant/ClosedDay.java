package com.application.persistence.model.restaurant;

import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "closed_day")
public class ClosedDay {
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	LocalDate closedDate;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "restaurant_id")
	Restaurant restaurant;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public LocalDate getClosedDate() {
		return closedDate;
	}
	public void setClosedDate(LocalDate day) {
		this.closedDate = day;
	}
	public Restaurant getRestaurant() {
		return restaurant;
	}
	public void setRestaurant(Restaurant restaurnat) {
		this.restaurant = restaurnat;
	}
}
