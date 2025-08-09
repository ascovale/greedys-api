package com.application.admin.service;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.application.common.persistence.mapper.CustomerDTOMapper;
import com.application.common.persistence.mapper.PrivilegeDTOMapper;
import com.application.common.persistence.mapper.RoleDTOMapper;
import com.application.common.web.dto.customer.CustomerDTO;
import com.application.common.web.dto.security.PrivilegeDTO;
import com.application.common.web.dto.security.RoleDTO;
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
    private final CustomerDTOMapper customerDTOMapper;
    private final RoleDTOMapper roleDTOMapper;
    private final PrivilegeDTOMapper privilegeDTOMapper;

    public void updateCustomerStatus(Long customerId, Customer.Status newStatus) {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        customer.setStatus(newStatus);
        customerDAO.save(customer);
    }

    public void updateCustomerStatusByEmail(String email, Customer.Status newStatus) {
        Customer customer = customerDAO.findByEmail(email);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found with email: " + email);
        }

        customer.setStatus(newStatus);
        customerDAO.save(customer);
    }

    public Page<CustomerDTO> findAll(PageRequest pageable) {
        return customerDAO.findAll(pageable).map(customerDTOMapper::toDTO);
    }

    public Page<RoleDTO> findAllRoles(PageRequest pageable) {
        return roleDAO.findAll(pageable).map(roleDTOMapper::toDTO);
    }

    public Page<PrivilegeDTO> findAllPrivileges(PageRequest pageable) {
        return privilegeDAO.findAll(pageable).map(privilegeDTOMapper::toDTO);
    }

    public RoleDTO getRoleById(Long roleId) {
        Role role = roleDAO.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        return roleDTOMapper.toDTO(role);
    }

    public PrivilegeDTO getPrivilegeById(Long privilegeId) {
        Privilege privilege = privilegeDAO.findById(privilegeId)
                .orElseThrow(() -> new EntityNotFoundException("Privilege not found"));
        return privilegeDTOMapper.toDTO(privilege);
    }

    public RoleDTO createRole(RoleDTO roleDTO) {
        Role role = roleDTOMapper.toEntity(roleDTO);
        Role savedRole = roleDAO.save(role);
        return roleDTOMapper.toDTO(savedRole);
    }

    public PrivilegeDTO createPrivilege(PrivilegeDTO privilegeDTO) {
        Privilege privilege = privilegeDTOMapper.toEntity(privilegeDTO);
        Privilege savedPrivilege = privilegeDAO.save(privilege);
        return privilegeDTOMapper.toDTO(savedPrivilege);
    }

    public RoleDTO updateRole(Long roleId, RoleDTO roleDTO) {
        Role role = roleDAO.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        roleDTOMapper.updateEntityFromDTO(roleDTO, role);
        Role savedRole = roleDAO.save(role);
        return roleDTOMapper.toDTO(savedRole);
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
