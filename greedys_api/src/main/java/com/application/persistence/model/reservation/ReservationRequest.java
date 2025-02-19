package com.application.persistence.model.reservation;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.restaurant.user.RestaurantUser;

import jakarta.persistence.Column;
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

@Entity
@Table(name = "reservation_request")
public class ReservationRequest {
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	/* IL RISTORANTE NON PUO ESSERE CAMBIATO
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_restaurant")
	private Restaurant restaurant;*/
	@DateTimeFormat(pattern = "yyyy/MM/dd")
	@Temporal(TemporalType.DATE)
	@Column(name = "r_date")
	private LocalDate date;
	@Column(name = "creation_date")
	private LocalDate creationDate;
	private ClientInfo user_info;
	@ManyToOne(optional = true)
	@JoinColumn(name = "user_id")
	private Customer user;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idslot")
	private	Slot slot;
	private Integer pax;
	private Integer kids = 0;
	private String notes;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idrestaurant_user")
 	private RestaurantUser restaurantUser;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idreservation", referencedColumnName = "id")
 	private Reservation reservation;


	@ManyToOne(fetch = FetchType.LAZY)
	private com.application.persistence.model.restaurant.Table table;

	public com.application.persistence.model.restaurant.Table getTable() {
		return table;
	}

	public void setTable(com.application.persistence.model.restaurant.Table table) {
		this.table = table;
	}


	public ClientInfo get_user_info() {
		return user_info;
	}

	public void set_user_info(ClientInfo user_info) {
		this.user_info = user_info;
	}

	public Long get_user_id() {
		if (user == null) {
			throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
		}
		return user.getId();
	}

	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public Long getId() {
		return id;
	}

	public Integer getPax() {
		return pax;
	}

	public void setPax(Integer pax) {
		this.pax = pax;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public Slot getSlot() {
		return slot;
	}

	public void setSlot(Slot slot) {
		this.slot = slot;
	}

	public Integer getKids() {
		return kids;
	}

	public void setKids(Integer kids) {
		this.kids = kids;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public RestaurantUser getRestaurantUser() {
		return restaurantUser;
	}

	public void setRestaurantUser(RestaurantUser restaurantUser) {
		this.restaurantUser = restaurantUser;
	}

	public Customer getUser() {
		return user;
	}

	public void setUser(Customer user) {
		this.user = user;
	}
	
	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}
}
