package com.application.spring.dataloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.admin.AdminDAO;
import com.application.persistence.dao.admin.AdminPrivilegeDAO;
import com.application.persistence.dao.admin.AdminRoleDAO;
import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminPrivilege;
import com.application.persistence.model.admin.AdminRole;
import com.application.service.AdminService;
import com.application.web.dto.post.admin.NewAdminDTO;

@Component
public class AdminSetup {

    @Autowired
    private AdminPrivilegeDAO adminPrivilegeDAO;
    @Autowired
    private AdminRoleDAO adminRoleDAO;
    @Autowired
    private AdminDAO adminDAO;
    @Autowired
    private AdminService adminService;

    private static final Logger logger = LoggerFactory.getLogger(AdminSetup.class);

    @Transactional
    public void setupAdminRolesAndPrivileges() {
        logger.info(">>> --- Admin Setup --- <<<");
        final AdminPrivilege adminReservationCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE");
        final AdminPrivilege adminReservationCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ");
        final AdminPrivilege adminReservationRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_WRITE");
        final AdminPrivilege adminReservationRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantUserRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_USER_READ");
        final AdminPrivilege adminRestaurantUserWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE");
        final AdminPrivilege adminSwitchToRestaurantUserAdmin = createAdminPrivilegeIfNotFound("PRIVILEGE_SWITCH_TO_RESTAURANT_USER_ADMIN");
        final AdminPrivilege adminRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_WRITE");
        final AdminPrivilege adminCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_READ");
        final AdminPrivilege adminCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_WRITE");

        final List<AdminPrivilege> adminPrivileges = new ArrayList<>(Arrays.asList(
            adminReservationCustomerWrite, adminReservationCustomerRead,
            adminReservationRestaurantWrite, adminReservationRestaurantRead,
            adminRestaurantUserRead, adminRestaurantUserWrite,
            adminRestaurantRead, adminRestaurantWrite,
            adminCustomerRead, adminCustomerWrite, adminSwitchToRestaurantUserAdmin));

        createAdminRoleIfNotFound("ROLE_SUPER_ADMIN", adminPrivileges);
        createAdminRoleIfNotFound("ROLE_ADMIN_MANAGER", new ArrayList<>(Arrays.asList(
            adminReservationCustomerRead, adminReservationRestaurantRead,
            adminRestaurantUserRead, adminRestaurantRead,
            adminCustomerRead)));
        createAdminRoleIfNotFound("ROLE_ADMIN_EDITOR", new ArrayList<>(Arrays.asList(
            adminReservationCustomerWrite, adminReservationRestaurantWrite,
            adminRestaurantUserWrite, adminRestaurantWrite,
            adminCustomerWrite)));
        logger.info(">>> --- Admin Setup finished --- <<<");
    }

    @Transactional
    public void createSomeAdmin() {
        logger.info(">>> --- Creating Admin --- <<<");
        Admin existingAdmin = adminDAO.findByEmail("ascolesevalentino@gmail.com");
        if (existingAdmin != null) {
            logger.info("Admin with email ascolesevalentino@gmail.com already exists.");
            return;
        }

        logger.info("Creating admin Valentino Ascolese");
        NewAdminDTO adminDTO = new NewAdminDTO();
        adminDTO.setEmail("ascolesevalentino@gmail.com");
        adminDTO.setFirstName("Valentino");
        adminDTO.setLastName("Ascolese");
        adminDTO.setPassword("Minosse100%");
        Admin admin =adminService.registerNewAdminAccount(adminDTO);
        adminService.updateAdminStatus(admin.getId(), Admin.Status.ENABLED);

        logger.info("Creating admin Matteo Rossi");
        NewAdminDTO admin2DTO = new NewAdminDTO();
        admin2DTO.setEmail("matteo.rossi1902@gmail.com");
        admin2DTO.setFirstName("Matteo");
        admin2DTO.setLastName("Rossi");
        admin2DTO.setPassword("Minosse100%");
        admin =adminService.registerNewAdminAccount(admin2DTO);
        adminService.updateAdminStatus(admin.getId(), Admin.Status.ENABLED);
    }

    @Transactional
    private AdminPrivilege createAdminPrivilegeIfNotFound(final String name) {
        AdminPrivilege privilege = adminPrivilegeDAO.findByName(name);
        if (privilege == null) {
            privilege = new AdminPrivilege(name);
            privilege = adminPrivilegeDAO.save(privilege);
        }
        return privilege;
    }

    private AdminRole createAdminRoleIfNotFound(String name, List<AdminPrivilege> privileges) {
        AdminRole role = adminRoleDAO.findByName(name);
        if (role == null) {
            role = new AdminRole(name);
        }
        // Ensure privileges is always a mutable list
        role.setAdminPrivileges(new ArrayList<>(privileges));
        adminRoleDAO.save(role);
        return role;
    }

    
    @Transactional
    public void adminSetup() {
        logger.info(">>> --- Admin Setup --- <<<");
        final AdminPrivilege adminReservationCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE");
        final AdminPrivilege adminReservationCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ");
        final AdminPrivilege adminReservationRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_WRITE");
        final AdminPrivilege adminReservationRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantUserRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_USER_READ");
        final AdminPrivilege adminRestaurantUserWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE");
        final AdminPrivilege adminSwitchToRestaurantUserAdmin = createAdminPrivilegeIfNotFound("PRIVILEGE_SWITCH_TO_RESTAURANT_USER_ADMIN");
        final AdminPrivilege adminRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_WRITE");
        final AdminPrivilege adminCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_READ");
        final AdminPrivilege adminCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_WRITE");

        final List<AdminPrivilege> adminPrivileges = new ArrayList<>(Arrays.asList(
            adminReservationCustomerWrite, adminReservationCustomerRead,
            adminReservationRestaurantWrite, adminReservationRestaurantRead,
            adminRestaurantUserRead, adminRestaurantUserWrite,
            adminRestaurantRead, adminRestaurantWrite,
            adminCustomerRead, adminCustomerWrite, adminSwitchToRestaurantUserAdmin));

        createAdminRoleIfNotFound("ROLE_SUPER_ADMIN", new ArrayList<>(adminPrivileges));
        createAdminRoleIfNotFound("ROLE_ADMIN_MANAGER", new ArrayList<>(Arrays.asList(
            adminReservationCustomerRead, adminReservationRestaurantRead,
            adminRestaurantUserRead, adminRestaurantRead,
            adminCustomerRead)));
        createAdminRoleIfNotFound("ROLE_ADMIN_EDITOR", new ArrayList<>(Arrays.asList(
            adminReservationCustomerWrite, adminReservationRestaurantWrite,
            adminRestaurantUserWrite, adminRestaurantWrite,
            adminCustomerWrite)));
        logger.info(">>> --- Admin Setup finished --- <<<");
    }
}