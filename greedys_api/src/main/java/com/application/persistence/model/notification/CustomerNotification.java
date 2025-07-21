package com.application.persistence.model.notification;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.application.persistence.model.customer.Customer;

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

@Entity
@Table(name = "notification")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
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
