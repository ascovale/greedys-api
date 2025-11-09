package com.application.customer.persistence.dao;

import java.util.List;
import java.util.Optional;

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

    Customer findByEmail(String email);

    /**
     * Find customer by phone number
     */
    Customer findByPhoneNumber(String phoneNumber);

    /**
     * Find customer by phone number and status
     */
    Optional<Customer> findByPhoneNumberAndStatus(String phoneNumber, Customer.Status status);

    /**
     * Find customer by email and status
     */
    Optional<Customer> findByEmailAndStatus(String email, Customer.Status status);

    /**
     * Find all unregistered customers (contacts made via phone reservations)
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'UNREGISTERED' ORDER BY c.name")
    List<Customer> findUnregisteredContacts();

    /**
     * Check if phone number exists for any customer
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Check if email exists for any customer
     */
    boolean existsByEmail(String email);

    @Query("SELECT a FROM Customer c JOIN c.allergies a WHERE c = :customer")
    Page<Allergy> findCustomerAllergies(@Param("customer") Customer customer, PageRequest pageRequest);

}
