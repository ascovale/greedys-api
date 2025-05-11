package com.application.persistence.dao.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.customer.Allergy;
import com.application.persistence.model.customer.Customer;


@Repository
public interface CustomerDAO extends JpaRepository<Customer, Long> {

    public Customer findByEmail(String email);

    @Query("SELECT a FROM Customer c JOIN c.allergies a WHERE c = :customer")
    Page<Allergy> findCustomerAllergies(@Param("customer") Customer customer, PageRequest pageRequest);

}