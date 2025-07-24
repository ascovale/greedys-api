package com.application.admin.service;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.application.admin.service.authentication.AdminCustomerAuthenticationService;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.PrivilegeDAO;
import com.application.customer.persistence.dao.RoleDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.Privilege;
import com.application.customer.persistence.model.Role;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminCustomerService {
    private final CustomerDAO customerDAO;
    private final RoleDAO roleDAO;
    private final PrivilegeDAO privilegeDAO;
    private final AdminCustomerAuthenticationService adminCustomerAuthenticationService;

    public void updateCustomerStatus(Long customerId, Customer.Status newStatus) {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        customer.setStatus(newStatus);
        customerDAO.save(customer);
    }

    public Page<CustomerDTO> findAll(PageRequest pageable) {
        return customerDAO.findAll(pageable).map(CustomerDTO::new);
    }

    public void addRoleToCustomer(Long customerId, String role) {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        Hibernate.initialize(customer.getRoles());
        customer.getRoles().add(roleDAO.findByName(role));
        customerDAO.save(customer);
    }

    public void removeRoleFromCustomer(Long customerId, String role) {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        customer.getRoles().remove(roleDAO.findByName(role));
        customerDAO.save(customer);
    }

    public void addPrivilegeToRole(String roleName, String privilegeName) {
        Role role = roleDAO.findByName(roleName);
        if (role == null) {
            throw new EntityNotFoundException("Role not found");
        }
        Privilege privilege = privilegeDAO.findByName(privilegeName);
        if (privilege == null) {
            throw new EntityNotFoundException("Permission not found");
        }
        Hibernate.initialize(role.getPrivileges());
        role.getPrivileges().add(privilege);
        roleDAO.save(role);
    }

    public void removePrivilegeFromRole(String roleName, String privilegeName) {
        Role role = roleDAO.findByName(roleName);
        if (role == null) {
            throw new EntityNotFoundException("Role not found");
        }
        Privilege privilege = privilegeDAO.findByName(privilegeName);
        if (privilege == null) {
            throw new EntityNotFoundException("Permission not found");
        }
        Hibernate.initialize(role.getPrivileges());
        role.getPrivileges().remove(privilege);
        roleDAO.save(role);
    }
}
