package com.application.spring;

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

import com.application.persistence.dao.customer.PrivilegeDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
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
    private PrivilegeDAO privilegeDAO;
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
     // == create initial privileges
        final Privilege ureadPrivilege = ucreatePrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = ucreatePrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = ucreatePrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

     // == create initial roles
        final List<Privilege> uadminPrivileges = new ArrayList<Privilege>(Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<Privilege>(Arrays.asList(ureadPrivilege, upasswordPrivilege));
        ucreateRoleIfNotFound("ROLE_ADMIN", uadminPrivileges);
        ucreateRoleIfNotFound("ROLE_USER", userPrivileges);
        // == create initial restaurant roles
        final List<Privilege> restaurantPrivileges = new ArrayList<Privilege>(Arrays.asList(ureadPrivilege, uwritePrivilege));
        
        ucreateRestaurantRoleIfNotFound("ROLE_OWNER", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_MANAGER", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_VIEWER", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_CHEF", restaurantPrivileges);
        ucreateRestaurantRoleIfNotFound("ROLE_WAITER", restaurantPrivileges);
        // == create initial companyUser
        // createUserIfNotFound("test@test.com", "Test", "Test", "test", new ArrayList<RoleCompany>(Arrays.asList(adminRole)));

        // create defautlt service types
        ucreateServiceIfNotFound("MENU");
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
	
    private Role ucreateRoleIfNotFound(String name, List<Privilege> privileges) {
    	Role role = roleDAO.findByName(name);
        if (role == null) {
            role = new Role(name);
        }
        role.setPrivileges(privileges);
        role = roleDAO.save(role);
        return role;
	}
    private Role ucreateRestaurantRoleIfNotFound(String name, List<Privilege> privileges) {
        Role role = roleDAO.findByName(name);
        if (role == null) {
            role = new Role(name);
        }
        role.setPrivileges(privileges);
        role = roleDAO.save(role);
        return role;
    }
}