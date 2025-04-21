package com.application.spring.dataloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.PrivilegeDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Privilege;
import com.application.persistence.model.customer.Role;
import com.application.service.CustomerService;
import com.application.web.dto.post.NewCustomerDTO;

@Component
public class CustomerSetup {

    @Autowired
    private PrivilegeDAO privilegeDAO;
    @Autowired
    private RoleDAO roleDAO;
    @Autowired
    private CustomerDAO userDAO;
    @Autowired
    private CustomerService userService;

    private static final Logger logger = LoggerFactory.getLogger(CustomerSetup.class);

    @Transactional
    public void setupCustomerRolesAndPrivileges() {
        logger.info(">>> --- Customer Setup --- <<<");
        final Privilege ureadPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = createPrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

        final List<Privilege> uadminPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, upasswordPrivilege));
        createRoleIfNotFound("ROLE_PREMIUM_USER", new ArrayList<>(uadminPrivileges));
        createRoleIfNotFound("ROLE_USER", new ArrayList<>(userPrivileges));
        logger.info(">>> --- Customer Setup finished --- <<<");
    }

    @Transactional
    public void createSomeCustomer() {
        logger.info(">>> --- Creating customer --- <<<");
        Customer existingCustomer = userDAO.findByEmail("info@lasoffittarenovatio.it");
        if (existingCustomer != null) {
            logger.info("Customer with email info@lasoffittarenovatio.it already exists.");
            return;
        }

        Customer user = null;
        try {
            NewCustomerDTO userDTO = new NewCustomerDTO();
            userDTO.setFirstName("Stefano");
            userDTO.setLastName("Di Michele");
            userDTO.setPassword("Minosse100%");
            userDTO.setEmail("info@lasoffittarenovatio.it");
            user = userService.registerNewCustomerAccount(userDTO);
        } catch (Exception e) {
            logger.error("Error creating customer account", e);
            return;
        }

        try {
            if (user == null) {
                return;
            }
            user = userDAO.findByEmail("info@lasoffittarenovatio.it");
            Role premiumRole = roleDAO.findByName("ROLE_PREMIUM_USER");
            if (premiumRole != null) {
                Hibernate.initialize(user.getRoles());
                List<Role> roles = new ArrayList<>(user.getRoles());
                if (!roles.contains(premiumRole)) {
                    roles.add(premiumRole);
                }
                user.setRoles(roles);
                user.setStatus(Customer.Status.ENABLED);
                userDAO.save(user);
            }
        } catch (Exception e) {
            logger.error("Error assigning roles to customer", e);
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
        role.setPrivileges(privileges);
        role = roleDAO.save(role);
        return role;
    }

    
    public void customerSetup() {
        logger.info(">>> --- Customer Setup --- <<<");
        final Privilege ureadPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = createPrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

        final List<Privilege> uadminPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<>(Arrays.asList(ureadPrivilege, upasswordPrivilege));
        createRoleIfNotFound("ROLE_PREMIUM_USER", new ArrayList<>(uadminPrivileges));
        createRoleIfNotFound("ROLE_USER", new ArrayList<>(userPrivileges));
        logger.info(">>> --- Customer Setup finished --- <<<");
    }


}