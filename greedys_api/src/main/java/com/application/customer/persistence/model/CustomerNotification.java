package com.application.customer.persistence.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.application.common.persistence.model.notification.ANotification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity(name = "CustomerNotificationEntity")
@Table(name = "notification")
public class CustomerNotification extends ANotification {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "idcustomer")
	private Customer customer;

	@Column(name = "creation_time", updatable = false)
	@CreationTimestamp
	private Instant creationTime;

}
