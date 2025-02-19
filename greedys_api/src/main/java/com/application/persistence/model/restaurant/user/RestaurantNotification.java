package com.application.persistence.model.restaurant.user;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.application.persistence.model.reservation.Reservation;

@Entity
@Table(name="notification_restaurant")
public class RestaurantNotification {
	
	public enum Type {NEW_RESERVATION,REQUEST, MODIFICATION, REVIEW, REVIEW_ALTERED, SEATED, NO_SHOW, CANCEL, ALTERED, ACCEPTED, REJECTED, USERNOTACCEPTEDRESERVATION};
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	@Column(name = "opened", columnDefinition = "TINYINT(1)")
	private Boolean opened;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "restaurantuser_id")
	private RestaurantUser restaurantUser;
	@Column(name = "n_type")
	private Type type;

	public RestaurantUser getRestaurantUser() {
		return restaurantUser;
	}

	public void setRestaurantUser(RestaurantUser restaurantUser) {
		this.restaurantUser = restaurantUser;
	}

	public RestaurantNotification() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	public Boolean getOpened() {
		return opened;
	}

	public void setOpened(Boolean opened) {
		this.opened = opened;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

}
