package com.application.admin.model;

import java.sql.Timestamp;

import com.application.common.persistence.model.reservation.Reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_notification")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {
	public enum Type {
		NEW_RESERVATION, REQUEST_RESERVATION, NO_SHOW, SEATED, UNSEATED, CANCEL, CONFIRMED, ALTERED, ACCEPTED, REJECTED
	};

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(name = "n_type")
	private Type type;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idcustomer")
	private Admin admin;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idreservation")
	private Reservation reservation;

	private String text;
	@Column(name = "creation_time")
	private Timestamp creationTime;

}
