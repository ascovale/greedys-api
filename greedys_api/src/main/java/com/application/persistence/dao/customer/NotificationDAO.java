package com.application.persistence.dao.customer;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Notification;

@Repository
public interface NotificationDAO extends JpaRepository<Notification, Long>{
	//@Query("SELECT * FROM Notification n WHERE n.iduser = ?1")
	public List<Notification> findByUser(Customer User);

    Page<Notification> findByUserAndUnopenedTrue(Customer customer, Pageable pageable);

    public Page<Notification> findAllByCustomer(Customer customer, Pageable pageable);
}
