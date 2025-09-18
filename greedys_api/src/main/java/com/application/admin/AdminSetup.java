package com.application.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.persistence.dao.AdminPrivilegeDAO;
import com.application.admin.persistence.dao.AdminRoleDAO;
import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.AdminPrivilege;
import com.application.admin.persistence.model.AdminRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSetup {

    private final AdminPrivilegeDAO adminPrivilegeDAO;
    private final AdminRoleDAO adminRoleDAO;
    private final AdminDAO adminDAO;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void setupAdminRolesAndPrivileges() {
        log.info("👑 --- Admin Setup --- ");
        final AdminPrivilege adminReservationCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE");
        final AdminPrivilege adminReservationCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ");
        final AdminPrivilege adminReservationRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_WRITE");
        final AdminPrivilege adminReservationRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ");
        final AdminPrivilege adminRUserRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_USER_READ");
        final AdminPrivilege adminRUserWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE");
        final AdminPrivilege adminSwitchToRUserAdmin = createAdminPrivilegeIfNotFound("PRIVILEGE_SWITCH_TO_RESTAURANT_USER_ADMIN");
        final AdminPrivilege adminSwitchToCustomer = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_SWITCH_TO_CUSTOMER");
        final AdminPrivilege adminRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_WRITE");
        final AdminPrivilege adminCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_READ");
        final AdminPrivilege adminCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_WRITE");
        final AdminPrivilege adminAdminRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_ADMIN_READ");
        final AdminPrivilege adminAdminWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_ADMIN_WRITE");

        final List<AdminPrivilege> adminPrivileges = new ArrayList<>(Arrays.asList(
            adminReservationCustomerWrite, adminReservationCustomerRead,
            adminReservationRestaurantWrite, adminReservationRestaurantRead,
            adminRUserRead, adminRUserWrite,
            adminRestaurantRead, adminRestaurantWrite,
            adminCustomerRead, adminCustomerWrite, 
            adminAdminRead, adminAdminWrite,
            adminSwitchToRUserAdmin, adminSwitchToCustomer));

        createAdminRoleIfNotFound("ROLE_SUPER_ADMIN", adminPrivileges);
        createAdminRoleIfNotFound("ROLE_ADMIN_MANAGER", new ArrayList<>(Arrays.asList(
            adminReservationCustomerRead, adminReservationRestaurantRead,
            adminRUserRead, adminRestaurantRead,
            adminCustomerRead)));
        createAdminRoleIfNotFound("ROLE_ADMIN_EDITOR", new ArrayList<>(Arrays.asList(
            adminReservationCustomerWrite, adminReservationRestaurantWrite,
            adminRUserWrite, adminRestaurantWrite,
            adminCustomerWrite)));
        log.info("✅ --- Admin Setup finished --- ");
    }

    @Transactional
    public void createSomeAdmin() {
        log.info("👤 --- Creating Admin --- ");

        Admin existingAdmin = adminDAO.findByEmail("ascolesevalentino@gmail.com");
        if (existingAdmin != null) {
            log.info("ℹ️ Admin with email ascolesevalentino@gmail.com already exists.");
            return;
        }

        log.info("👨‍💼 Creating admin Valentino Ascolese");
        Admin admin = Admin.builder()
            .email("ascolesevalentino@gmail.com")
            .name("Valentino")
            .surname("Ascolese")
            .password(passwordEncoder.encode("Minosse100%"))
            .status(Admin.Status.ENABLED)
            .build();
        admin = adminDAO.save(admin);

        log.info("👨‍💼 Creating admin Matteo Rossi");
        Admin admin2 = Admin.builder()
            .email("matteo.rossi1902@gmail.com")
            .name("Matteo")
            .surname("Rossi")
            .password(passwordEncoder.encode("Minosse100%"))
            .status(Admin.Status.ENABLED)
            .build();
        admin2 = adminDAO.save(admin2);

        Admin admin3 = Admin.builder()
            .email("test@test.com")
            .name("Test")
            .surname("Test")
            .password(passwordEncoder.encode("Test100%%%"))
            .status(Admin.Status.ENABLED)
            .build();
        admin3 = adminDAO.save(admin3);
        
        // Assign SUPER_ADMIN role to all admins
        AdminRole superAdminRole = adminRoleDAO.findByName("ROLE_SUPER_ADMIN");
        if (superAdminRole != null) {
            admin.setAdminRoles(new ArrayList<>(Arrays.asList(superAdminRole)));
            admin2.setAdminRoles(new ArrayList<>(Arrays.asList(superAdminRole)));
            admin3.setAdminRoles(new ArrayList<>(Arrays.asList(superAdminRole)));
            adminDAO.save(admin);
            adminDAO.save(admin2);
            adminDAO.save(admin3);
            log.info("🔑 Assigned ROLE_SUPER_ADMIN to all admins");
        } else {
            log.warn("⚠️ ROLE_SUPER_ADMIN not found - admins may not have proper permissions");
        }
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
}
