package com.application.spring.dataloader;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.SetupConfigDAO;
import com.application.persistence.dao.admin.AdminDAO;
import com.application.persistence.dao.admin.AdminPrivilegeDAO;
import com.application.persistence.dao.admin.AdminRoleDAO;
import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.PrivilegeDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantPrivilegeDAO;
import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminPrivilege;
import com.application.persistence.model.admin.AdminRole;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Privilege;
import com.application.persistence.model.customer.Role;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantRole;
import com.application.persistence.model.systemconfig.SetupConfig;
import com.application.service.AdminService;
import com.application.service.CustomerService;
import com.application.service.RestaurantService;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.dto.post.NewRestaurantDTO;
import com.application.web.dto.post.admin.NewAdminDTO;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

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
    private PasswordEncoder passwordEncoder;
    @Autowired
    RestaurantRoleDAO restaurantRoleDAO;
    @Autowired
    RestaurantPrivilegeDAO restaurantPrivilegeDAO;
    @Autowired
    private SetupConfigDAO setupConfigDAO;
    @Autowired
    private AdminDAO adminDAO;
    @Autowired
    private CustomerDAO userDAO;
    @Autowired
    private CustomerService userService;
    @Autowired
    private RestaurantDAO restaurantDAO;
    @Autowired
    private ServiceDAO serviceDAO;
    @Autowired
    private SlotDAO slotDAO;
    @Autowired
    private RestaurantUserDAO ruDAO;
    @Autowired
    AdminService adminService;
    @Autowired
    RestaurantService restaurantService;

    static final Logger logger = LoggerFactory.getLogger(SetupDataLoader.class);

    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {

        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (!setupConfig.isAlreadySetup()) {

            logger.info("    >>>  ---   Setup started   ---  <<< ");
            setupConfig = new SetupConfig();
            setupConfig.setId(1L);
            setupConfig.setAlreadySetup(true);
            setupConfigDAO.save(setupConfig);
            createDefaultServiceTypes();
            adminSetup();
            customerSetup();
            createRestaurantPrivilegesAndRoles();
            logger.info("    >>>  ---   Setup finished   ---  <<< ");
        }
        if (!setupConfig.isDataUploaded()) {

            logger.info("    >>>  ---   Creating Test data  ---  <<< ");
            setupConfig.setAlreadySetup(true);
            setupConfig.setDataUploaded(true);
            createSomeAdmin();
            createSomeCustomer();
            createRestaurantLaSoffittaRenovatio();
            logger.info("    >>>  ---   Test data Created   ---  <<< ");
        }
    }

    @Transactional
    public void adminSetup() {
        logger.info("    >>>  ---   Admin Setup   ---  <<< ");
        // == ADMIN: create initial privileges
        final AdminPrivilege adminReservationCustomerWrite = createAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_WRITE");
        final AdminPrivilege adminReservationCustomerRead = createAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_CUSTOMER_READ");
        final AdminPrivilege adminReservationRestaurantWrite = createAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_WRITE");
        final AdminPrivilege adminReservationRestaurantRead = createAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESERVATION_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantUserRead = createAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESTAURANT_USER_READ");
        final AdminPrivilege adminRestaurantUserWrite = createAdminPrivilegeIfNotFound(
                "PRIVILEGE_ADMIN_RESTAURANT_USER_WRITE");
        final AdminPrivilege adminRestaurantRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_READ");
        final AdminPrivilege adminRestaurantWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_RESTAURANT_WRITE");
        final AdminPrivilege adminCustomerRead = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_READ");
        final AdminPrivilege adminCustomerWrite = createAdminPrivilegeIfNotFound("PRIVILEGE_ADMIN_CUSTOMER_WRITE");

        // == ADMIN: Create initial admin roles
        final List<AdminPrivilege> adminPrivileges = new ArrayList<>(Arrays.asList(
                adminReservationCustomerWrite, adminReservationCustomerRead,
                adminReservationRestaurantWrite, adminReservationRestaurantRead,
                adminRestaurantUserRead, adminRestaurantUserWrite,
                adminRestaurantRead, adminRestaurantWrite,
                adminCustomerRead, adminCustomerWrite));

        createAdminRoleIfNotFound("ROLE_SUPER_ADMIN", new ArrayList<>(adminPrivileges));
        createAdminRoleIfNotFound("ROLE_ADMIN_MANAGER", new ArrayList<>(Arrays.asList(
                adminReservationCustomerRead, adminReservationRestaurantRead,
                adminRestaurantUserRead, adminRestaurantRead,
                adminCustomerRead)));
        createAdminRoleIfNotFound("ROLE_ADMIN_EDITOR", new ArrayList<>(Arrays.asList(
                adminReservationCustomerWrite, adminReservationRestaurantWrite,
                adminRestaurantUserWrite, adminRestaurantWrite,
                adminCustomerWrite)));
        logger.info("    >>>  ---   Admin Setup finished  ---  <<< ");

    }

    public void customerSetup() {
        logger.info("    >>>  ---   Customer Setup   ---  <<< ");

        // == CUSTOMER: create initial customer privileges
        final Privilege ureadPrivilege = createPrivilegeIfNotFound("READ_PRIVILEGE");
        final Privilege uwritePrivilege = createPrivilegeIfNotFound("WRITE_PRIVILEGE");
        final Privilege upasswordPrivilege = createPrivilegeIfNotFound("CHANGE_PASSWORD_PRIVILEGE");

        // == CUSTOMER: create initial customer roles
        final List<Privilege> uadminPrivileges = new ArrayList<Privilege>(
                Arrays.asList(ureadPrivilege, uwritePrivilege, upasswordPrivilege));
        final List<Privilege> userPrivileges = new ArrayList<Privilege>(
                Arrays.asList(ureadPrivilege, upasswordPrivilege));
        createRoleIfNotFound("ROLE_PREMIUM_USER", new ArrayList<>(uadminPrivileges));
        createRoleIfNotFound("ROLE_USER", new ArrayList<>(userPrivileges));
        logger.info("    >>>  ---   Customer Setup finished   ---  <<< ");

    }

    @Transactional
    public void createDefaultServiceTypes() {
        logger.info("    >>>  ---   Creating Default Service Types   ---  <<< ");
        createServiceIfNotFound("Lunch");
        createServiceIfNotFound("Dinner");
        createServiceIfNotFound("Aperitif");
        createServiceIfNotFound("Breakfast");
        createServiceIfNotFound("After Dinner");
        logger.info("    >>>  ---   Default Service Types Created   ---  <<< ");
    }

    @Transactional
    private final ServiceType createServiceIfNotFound(String name) {
        ServiceType serviceType = serviceTypeDAO.findByName(name);
        if (serviceType == null) {
            serviceType = new ServiceType(name);
            serviceTypeDAO.save(serviceType);
        }
        return serviceType;
    }

    @Transactional
    private final Privilege createPrivilegeIfNotFound(final String name) {
        Privilege privilege = privilegeDAO.findByName(name);
        if (privilege == null) {

            privilege = new Privilege(name);
            privilege = privilegeDAO.save(privilege);
        }
        return privilege;
    }

    @Transactional
    private final AdminPrivilege createAdminPrivilegeIfNotFound(final String name) {
        AdminPrivilege privilege = adminPrivilegeDAO.findByName(name);
        if (privilege == null) {
            privilege = new AdminPrivilege(name);
            privilege = adminPrivilegeDAO.save(privilege);
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

    private AdminRole createAdminRoleIfNotFound(String name, List<AdminPrivilege> privileges) {
        AdminRole role = adminRoleDAO.findByName(name);
        if (role == null) {
            role = new AdminRole(name);
        }
        role.setAdminPrivileges(privileges);
        role = adminRoleDAO.save(role);
        return role;
    }

    @Transactional
    private void createRestaurantPrivilegesAndRoles() {
        logger.info("    >>>  ---   Creating Restaurant Privileges and Roles   ---  <<< ");
        // Crea i privilegi specifici per i ruoli dei ristoranti
        final RestaurantPrivilege managerWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE");
        final RestaurantPrivilege chefWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_CHEF_WRITE");
        final RestaurantPrivilege waiterWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_WAITER_WRITE");
        final RestaurantPrivilege viewerWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_VIEWER_WRITE");
        final RestaurantPrivilege roleWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_ROLE_WRITE");
        final RestaurantPrivilege reservationWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE");
        final RestaurantPrivilege serviceWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE");
        final RestaurantPrivilege serviceReadPrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_SERVICE_READ");
        final RestaurantPrivilege slotWritePrivilege = createRestaurantPrivilegeIfNotFound(
                "PRIVILEGE_RESTAURANT_USER_SLOT_WRITE");

        // Lista dei privilegi per i ruoli dei ristoranti
        final List<RestaurantPrivilege> ownerPrivileges = new ArrayList<>(Arrays.asList(
                managerWritePrivilege, chefWritePrivilege, waiterWritePrivilege, viewerWritePrivilege,
                roleWritePrivilege, reservationWritePrivilege, serviceWritePrivilege, serviceReadPrivilege,
                slotWritePrivilege));

        final List<RestaurantPrivilege> managerPrivileges = new ArrayList<>(Arrays.asList(
                chefWritePrivilege, waiterWritePrivilege, viewerWritePrivilege,
                roleWritePrivilege, reservationWritePrivilege, serviceWritePrivilege, serviceReadPrivilege,
                slotWritePrivilege));

        final List<RestaurantPrivilege> viewerPrivileges = new ArrayList<>(Arrays.asList(
                serviceReadPrivilege));

        final List<RestaurantPrivilege> chefPrivileges = new ArrayList<>(Arrays.asList(
                serviceReadPrivilege, slotWritePrivilege));

        final List<RestaurantPrivilege> waiterPrivileges = new ArrayList<>(Arrays.asList(
                serviceReadPrivilege));

        // Crea i ruoli predefiniti per i ristoranti
        createRestaurantRoleIfNotFound("ROLE_OWNER", ownerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_MANAGER", managerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_VIEWER", viewerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_CHEF", chefPrivileges);
        createRestaurantRoleIfNotFound("ROLE_WAITER", waiterPrivileges);
        logger.info("    >>>  ---   Restaurant Privileges and Roles Created   ---  <<< ");
    }

    @Transactional
    private RestaurantPrivilege createRestaurantPrivilegeIfNotFound(final String name) {
        RestaurantPrivilege privilege = restaurantPrivilegeDAO.findByName(name);
        if (privilege == null) {
            privilege = new RestaurantPrivilege(name);
            privilege = restaurantPrivilegeDAO.save(privilege);
        }
        return privilege;
    }

    @Transactional
    private RestaurantRole createRestaurantRoleIfNotFound(String name, List<RestaurantPrivilege> privileges) {
        RestaurantRole role = restaurantRoleDAO.findByName(name);
        if (role == null) {
            role = new RestaurantRole(name);
        }
        role.setPrivileges(privileges);
        role = restaurantRoleDAO.save(role);
        return role;
    }

    @Transactional
    private void createSomeCustomer() {
        logger.info("    >>>  ---   Creating customer  ---  <<< ");
        Customer existingCustomer = userDAO.findByEmail("info@lasoffittarenovatio.it");
        if (existingCustomer != null) {
            System.out.println("Customer with email info@lasoffittarenovatio.it already exists.");
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
            e.printStackTrace();
        }
        try {
            if (user == null) {
                return;
            }
            user = userDAO.findByEmail("info@lasoffittarenovatio.it");
            Role premiumRole = roleDAO.findByName("ROLE_PREMIUM_USER");
            user.setStatus(Customer.Status.ENABLED);
            if (premiumRole != null) {
                logger.info("<<< User: {}", user);
                logger.info(">>> PremiumRole: {}", premiumRole);
                logger.info("Roles before adding: {}", user.getRoles());
                Hibernate.initialize(user.getRoles());
                // Crea una nuova lista modificabile
                List<Role> roles = new ArrayList<>(user.getRoles());
                if (!roles.contains(premiumRole)) {
                    roles.add(premiumRole);
                }
                user.setRoles(roles); // Sostituisci la collezione originale
                logger.info("Roles after adding: {}", user.getRoles());
            }
            userDAO.save(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    private void createSomeAdmin() {
        logger.info("    >>>  ---   Creating Admin  ---  <<< ");
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
        adminService.registerNewAdminAccount(adminDTO);

        logger.info("Creating admin Matteo Rossi");
        NewAdminDTO admin2DTO = new NewAdminDTO();
        admin2DTO.setEmail("matteo.rossi1902@gmail.com");
        admin2DTO.setFirstName("Matteo");
        admin2DTO.setLastName("Rossi");
        admin2DTO.setPassword("Minosse100%");
        adminService.registerNewAdminAccount(admin2DTO);
    }

    @Transactional
    private void createRestaurantLaSoffittaRenovatio() {
        logger.info("    >>>  ---   Creating Restaurant La Soffitta Renovatio  ---  <<< ");
        Restaurant restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        if (restaurant != null) {
            System.out.println("Restaurant with name La Soffitta Renovatio already exists.");
            return;
        }
        if (restaurant == null) {
            NewRestaurantDTO restaurantDto = new NewRestaurantDTO();
            restaurantDto.setName("La Soffitta Renovatio");
            restaurantDto.setAddress("Piazza del Risorgimento 46A");
            restaurantDto.setEmail("info@lasoffittarenovatio.it");
            restaurantDto.setPassword("Minosse100%");
            restaurantService.registerRestaurant(restaurantDto);

            ServiceType pranzoType = serviceTypeDAO.findByName("Lunch");
            Service pranzo = new Service();
            pranzo.addServiceType(pranzoType);
            pranzo.setValidFrom(LocalDate.now());
            pranzo.setValidTo(LocalDate.now());
            pranzo.setRestaurant(restaurant);
            serviceDAO.save(pranzo);
            createSlotsForService(pranzo, LocalTime.of(11, 0), LocalTime.of(17, 0));

            ServiceType cenaType = serviceTypeDAO.findByName("Dinner");
            Service cena = new Service();
            cenaType.setName("Cena");

            cena.setValidFrom(LocalDate.now());
            cena.setValidTo(LocalDate.now());
            cena.setRestaurant(restaurant);
            cena.addServiceType(cenaType);
            serviceDAO.save(cena);
            createSlotsForService(cena, LocalTime.of(17, 30), LocalTime.of(23, 0));

        }
    }

    private List<Slot> createSlotsForService(Service service, LocalTime startTime, LocalTime endTime) {
        List<Slot> slots = new ArrayList<>();
        LocalTime time = startTime;
        while (time.isBefore(endTime)) {
            for (int day = 1; day <= 7; day++) {
                Weekday weekday = Weekday.values()[day - 1];
                Slot slot = new Slot(time, time.plusMinutes(30));
                slot.setService(service);
                slot.setWeekday(weekday);
                slots.add(slot);
            }
            time = time.plusMinutes(30);
        }
        // Save slots to the database
        for (Slot slot : slots) {
            slotDAO.save(slot);
        }
        return slots;
    }
}
