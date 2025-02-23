package com.application.spring.dataloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.admin.AdminPrivilegeDAO;
import com.application.persistence.dao.admin.AdminRoleDAO;
import com.application.persistence.dao.customer.PrivilegeDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
import com.application.persistence.model.admin.AdminPrivilege;
import com.application.persistence.model.admin.AdminRole;
import com.application.persistence.model.customer.Privilege;
import com.application.persistence.model.customer.Role;
import com.application.persistence.model.reservation.ServiceType;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private boolean alreadySetup = false;
    @Autowired
    private ServiceTypeDAO serviceTypeDAO;
    @Autowired
    private RoleDAO roleDAO;
    @Autowired
    private AdminRoleDAO adminRoleDAO;
    @Autowired
    private PrivilegeDAO privilegeDAO;
    @Autowired
    private AdminPrivilegeDAO adminPrivilegeDAO;
    @Autowired
    @Qualifier("userEncoder")
    private PasswordEncoder passwordEncoder;

    // API
    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        if (alreadySetup) {
            return;
        }

        // == ADMIN: create initial privileges
        final AdminPrivilege adminReservationCustomerWrite = ucreateAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE");
        final AdminPrivilege adminReservationCustomerRead = ucreateAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ");
        final AdminPrivilege adminReservationRestaurantWrite = ucreateAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_WRITE");
        final AdminPrivilege adminReservationRestaurantRead = ucreateAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantUserRead = ucreateAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESTAURANT_USER_READ");
        final AdminPrivilege adminRestaurantUserWrite = ucreateAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE");
        final AdminPrivilege adminRestaurantRead = ucreateAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantWrite = ucreateAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_WRITE");
        final AdminPrivilege adminCustomerRead = ucreateAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_READ");
        final AdminPrivilege adminCustomerWrite = ucreateAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_WRITE");

        // == ADMIN: Create initial admin roles
        final List<AdminPrivilege> adminPrivileges = new ArrayList<>(Arrays.asList(
                adminReservationCustomerWrite, adminReservationCustomerRead,
                adminReservationRestaurantWrite, adminReservationRestaurantRead,
                adminRestaurantUserRead, adminRestaurantUserWrite,
                adminRestaurantRead, adminRestaurantWrite,
                adminCustomerRead, adminCustomerWrite));

        ucreateAdminRoleIfNotFound("ROLE_SUPER_ADMIN", adminPrivileges);
        ucreateAdminRoleIfNotFound("ROLE_ADMIN_MANAGER", Arrays.asList(
                adminReservationCustomerRead, adminReservationRestaurantRead,
                adminRestaurantUserRead, adminRestaurantRead,
                adminCustomerRead));
        ucreateAdminRoleIfNotFound("ROLE_ADMIN_EDITOR", Arrays.asList(
                adminReservationCustomerWrite, adminReservationRestaurantWrite,
                adminRestaurantUserWrite, adminRestaurantWrite,
                adminCustomerWrite));

        // == CUSTOMER: create initial customer privileges
        final Privilege ureadPrivilege = ucreatePrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = ucreatePrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = ucreatePrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

        // == CUSTOMER: create initial customer roles
        final List<Privilege> uadminPrivileges = new ArrayList<Privilege>(
                Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<Privilege>(
                Arrays.asList(ureadPrivilege, upasswordPrivilege));
        ucreateRoleIfNotFound("ROLE_PREMIUM_USER", uadminPrivileges);
        ucreateRoleIfNotFound("ROLE_USER", userPrivileges);

        // == I RESTAURANT ROLE SONO RELATIVI AD UN RISTORANTE QUINDI POSSONO ESSERE
        // GESTITI DALL'UTENTE E VENGONO GENERATI QUANDO VIENE CREATO UN RISTORANTE
        /* 
        final List<Privilege> restaurantPrivileges = new ArrayList<Privilege>(
                Arrays.asList(ureadPrivilege, uwritePrivilege));

        ucreateRestaurantRoleIfNotFound("ROLE_OWNER", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_MANAGER", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_VIEWER", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_CHEF", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_WAITER", restaurantPrivileges);*/
        // == create initial companyUser
        // createUserIfNotFound("test@test.com", "Test", "Test", "test", new
        // ArrayList<RoleCompany>(Arrays.asList(adminRole)));

        // create defautlt service types
        //ucreateServiceIfNotFound("MENU");
        alreadySetup = true;
    }

    @Transactional
    private final ServiceType ucreateServiceIfNotFound(String name) {
        ServiceType serviceType = serviceTypeDAO.findByName(name);
        if (serviceType != null) {
            serviceType = new ServiceType(name);
            serviceTypeDAO.save(serviceType);
        }
        return serviceType;
    }

    @Transactional
    private final Privilege ucreatePrivilegeIfNotFound(final String name) {
        Privilege privilege = privilegeDAO.findByName(name);
        if (privilege == null) {

            privilege = new Privilege(name);
            privilege = privilegeDAO.save(privilege);
        }
        return privilege;
    }

    @Transactional
    private final AdminPrivilege ucreateAdminPrivilegeIfNotFound(final String name) {
        AdminPrivilege privilege = adminPrivilegeDAO.findByName(name);
        if (privilege == null) {
            privilege = new AdminPrivilege(name);
            privilege = adminPrivilegeDAO.save(privilege);
        }
        return privilege;
    }

    private Role ucreateRoleIfNotFound(String name, List<Privilege> privileges) {
        Role role = roleDAO.findByName(name);
        if (role == null) {
            role = new Role(name);
        }
        role.setPrivileges(privileges);
        role = roleDAO.save(role);
        return role;
    }

    private AdminRole ucreateAdminRoleIfNotFound(String name, List<AdminPrivilege> privileges) {
        AdminRole role = adminRoleDAO.findByName(name);
        if (role == null) {
            role = new AdminRole(name);
        }
        role.setAdminPrivileges(privileges);
        role = adminRoleDAO.save(role);
        return role;
    }
    /* 
    private Role ucreateRestaurantRoleIfNotFound(String name, List<Privilege> privileges) {
        Role role = roleDAO.findByName(name);
        if (role == null) {
            role = new Role(name);
        }
        role.setPrivileges(privileges);
        role = roleDAO.save(role);
        return role;
    }*/
}