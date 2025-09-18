package com.application.customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.PrivilegeDAO;
import com.application.customer.persistence.dao.RoleDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.Privilege;
import com.application.customer.persistence.model.Role;
import com.application.customer.service.authentication.CustomerAuthenticationService;
import com.application.customer.web.dto.customer.NewCustomerDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerSetup {

    private final PrivilegeDAO privilegeDAO;
    private final RoleDAO roleDAO;
    private final CustomerDAO userDAO;
    private final CustomerAuthenticationService userService;

    @Transactional
    public void setupCustomerRolesAndPrivileges() {
        log.info("👥 --- Customer Setup --- ");
        final Privilege ureadPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = createPrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

        final List<Privilege> uadminPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, upasswordPrivilege));
        createRoleIfNotFound("ROLE_PREMIUM_USER", new ArrayList<>(uadminPrivileges));
        createRoleIfNotFound("ROLE_USER", new ArrayList<>(userPrivileges));
        log.info("✅ --- Customer Setup finished --- ");
    }

    @Transactional
    public void createSomeCustomer() {
        log.info("👤 --- Creating customer --- ");
        Customer existingCustomer = userDAO.findByEmail("info@lasoffittarenovatio.it");
        if (existingCustomer != null) {
            log.info("ℹ️ Customer with email info@lasoffittarenovatio.it already exists.");
            return;
        }

        try {
            log.info("👨‍💼 Creating customer Stefano Di Michele");
            NewCustomerDTO newUser = NewCustomerDTO.builder()
                .firstName("Stefano")
                .lastName("Di Michele")
                .password("Minosse100%")
                .email("info@lasoffittarenovatio.it")
                .build();
            Customer user = userService.registerNewCustomerAccount(newUser);
            user.setStatus(Customer.Status.ENABLED);
            Role premiumRole = roleDAO.findByName("ROLE_PREMIUM_USER");
            if (premiumRole != null) {
                Hibernate.initialize(user.getRoles());
                List<Role> roles = new ArrayList<>(user.getRoles());
                if (!roles.contains(premiumRole)) {
                    roles.add(premiumRole);
                }
                user.setRoles(roles);
                userDAO.save(user);
            }
        } catch (Exception e) {
            log.error("❌ Error creating customer account or assigning roles", e);
        }
    }

    @Transactional
    private Privilege createPrivilegeIfNotFound(final String name) {
        Privilege privilege = privilegeDAO.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilege = privilegeDAO.save(privilege);
        }
        return privilege;
    }

    private Role createRoleIfNotFound(String name, List<Privilege> privileges) {
        Role role = roleDAO.findByName(name);
        if (role == null) {
            role = new Role(name);
        }
        // Ensure privileges is always a mutable list
        role.setPrivileges(new ArrayList<>(privileges));
        role = roleDAO.save(role);
        return role;
    }

    
    public void customerSetup() {
        log.info("👥 --- Customer Setup --- ");
        final Privilege ureadPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = createPrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

        final List<Privilege> uadminPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, upasswordPrivilege));
        createRoleIfNotFound("ROLE_PREMIUM_USER", new ArrayList<>(uadminPrivileges));
        createRoleIfNotFound("ROLE_USER", new ArrayList<>(userPrivileges));
        log.info("✅ --- Customer Setup finished --- ");
    }


}
