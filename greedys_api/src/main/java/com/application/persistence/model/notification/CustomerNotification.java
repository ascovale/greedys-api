package com.application.persistence.model.notification;

import java.time.Instant;
import java.util.Map;

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

@Entity
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

	public CustomerNotification(Customer customer, String title, String body, Map<String, String> data) {
		super(title, body, data);
		this.customer = customer;
		
	}


	public Long getId() {
		return id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public Instant getCreationTime() {
		return creationTime;
	}
 
}
