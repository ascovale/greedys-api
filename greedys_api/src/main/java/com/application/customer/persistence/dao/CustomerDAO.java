package com.application.customer.persistence.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.customer.persistence.model.Allergy;
import com.application.customer.persistence.model.Customer;


@Repository
public interface CustomerDAO extends JpaRepository<Customer, Long> {

    public Customer findByEmail(String email);

    @Query("SELECT a FROM Customer c JOIN c.allergies a WHERE c = :customer")
    Page<Allergy> findCustomerAllergies(@Param("customer") Customer customer, PageRequest pageRequest);

}
