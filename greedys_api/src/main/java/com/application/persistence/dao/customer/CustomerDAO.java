package com.application.persistence.dao.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.user.Customer;


@Repository
public interface CustomerDAO extends  JpaRepository<Customer, Long>{

	public Customer findByEmail(String email);

}