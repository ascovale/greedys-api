package com.application.persistence.dao.customer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.notification.CustomerNotification;

@Repository
public interface NotificationDAO extends JpaRepository<CustomerNotification, Long>{
	//@Query("SELECT * FROM Notification n WHERE n.iduser = ?1")
	public List<CustomerNotification> findByCustomer(Customer customer);

    Page<CustomerNotification> findByCustomerAndUnopenedTrue(Customer customer, Pageable pageable);

    public Page<CustomerNotification> findAllByCustomer(Customer customer, Pageable pageable);
}
