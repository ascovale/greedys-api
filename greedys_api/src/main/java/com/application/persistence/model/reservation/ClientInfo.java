package com.application.persistence.model.reservation;

import jakarta.persistence.Embeddable;

@Embeddable
public record ClientInfo(String name, String surname, String email) {};