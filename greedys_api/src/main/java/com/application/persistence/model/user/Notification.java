package com.application.persistence.model.user;

import java.sql.Timestamp;

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
@Table(name = "notification")
public class Notification {
	public enum Type {
		NEW_RESERVATION, REQUEST_RESERVATION, NO_SHOW, SEATED, UNSEATED, CANCEL, CONFIRMED, ALTERED, ACCEPTED, REJECTED
	};

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(name = "n_type")
	private Type type;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "iduser")
	private Customer user;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idreservation")
	private Reservation reservation;
	@Column(columnDefinition = "TINYINT(1)")
	private Boolean unopened = true;
	private String text;
	@Column(name = "creation_time")
	private Timestamp creationTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Customer getClientUser() {
		return user;
	}

	public void setClientUser(Customer user) {
		this.user = user;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	public Boolean getUnopened() {
		return unopened;
	}

	public void setUnopened(Boolean unopened) {
		this.unopened = unopened;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Timestamp getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}

 
}
