package com.application.common.persistence.model.reservation;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.customer.model.Customer;
import com.application.restaurant.model.user.RUser;

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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
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

	@ManyToOne(optional = true)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idslot")
	private	Slot slot;

	private Integer pax;

	@Builder.Default
	private Integer kids = 0;

	private String notes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idrestaurant_user")
 	private RUser RUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idreservation", referencedColumnName = "id")
 	private Reservation reservation;

	@ManyToOne(fetch = FetchType.LAZY)
	private com.application.restaurant.model.Table table;




}
