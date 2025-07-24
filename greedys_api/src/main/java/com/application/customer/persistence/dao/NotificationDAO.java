package com.application.customer.persistence.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.CustomerNotification;

@Repository
public interface NotificationDAO extends JpaRepository<CustomerNotification, Long>{
	//@Query("SELECT * FROM Notification n WHERE n.iduser = ?1")
	public List<CustomerNotification> findByCustomer(Customer customer);

    Page<CustomerNotification> findByCustomerAndReadFalse(Customer customer, Pageable pageable);

    public Page<CustomerNotification> findAllByCustomer(Customer customer, Pageable pageable);
}
