package com.application.web.socket;

import java.io.Serializable;

public class RestaurantNotificationMessage extends OutputMessage implements Serializable{
    private static final long serialVersionUID = 1L;

	enum Type {
		CREATION, MODIFICATION, REVIEW, REVIEW_MODIFY
	};
	private Long id;
	private Type type;
	private Long reservationId;

	public RestaurantNotificationMessage() {
		super();
	}
	// Constructors
	public RestaurantNotificationMessage(String from,String to, String text, String time) {
		super(from, to, text, time);
	}

	public RestaurantNotificationMessage(String from, String to, String text, String time, 
											Long id, Long reservationId, Type type) {
		super(from, to, text, time);
		this.id = id;
		this.reservationId = reservationId;
		this.type = type;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getReservationId() {
		return reservationId;
	}

	public void setReservationId(Long reservationId) {
		this.reservationId = reservationId;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}
