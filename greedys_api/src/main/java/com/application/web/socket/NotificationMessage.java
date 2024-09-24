package com.application.web.socket;

import java.io.Serializable;

public class NotificationMessage extends OutputMessage implements Serializable{
    private static final long serialVersionUID = 1L;

    enum Type {
        NEW_RESERVATION, REQUEST_RESERVATION, NO_SHOW,SEATED,CANCEL,CONFIRMED,ALTERED
    };
    private String to;
    private Long id;
    private Boolean opened;
    private Long reservationId;
    private Type type;

    // Constructors
    public NotificationMessage(String from, String text, String time) {
        super(from, text, time);
    }
    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getOpened() {
        return opened;
    }

    public void setOpened(Boolean opened) {
        this.opened = opened;
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
