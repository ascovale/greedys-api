package com.application.admin.service;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.application.common.web.dto.get.CustomerDTO;
import com.application.customer.dao.CustomerDAO;
import com.application.customer.dao.PrivilegeDAO;
import com.application.customer.dao.RoleDAO;
import com.application.customer.model.Customer;
import com.application.customer.model.Privilege;
import com.application.customer.model.Role;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AdminCustomerService {
    private final CustomerDAO customerDAO;
    private final RoleDAO roleDAO;
    private final PrivilegeDAO privilegeDAO;

    public void updateCustomerStatus(Long customerId, Customer.Status newStatus) {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        customer.setStatus(newStatus);
        customerDAO.save(customer);
    }

    public Page<CustomerDTO> findAll(PageRequest pageable) {
        return customerDAO.findAll(pageable).map(CustomerDTO::new);
    }

    public Object adminLoginToCustomer(Long customerId, HttpServletRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'adminLoginToCustomer'");
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
