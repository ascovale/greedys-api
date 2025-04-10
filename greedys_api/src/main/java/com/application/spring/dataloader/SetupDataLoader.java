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
import com.application.persistence.dao.restaurant.RestaurantCategoryDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantPrivilegeDAO;
import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
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
import com.application.persistence.model.restaurant.RestaurantCategory;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantRole;
import com.application.persistence.model.systemconfig.SetupConfig;
import com.application.service.AdminService;
import com.application.service.AllergyService;
import com.application.service.CustomerService;
import com.application.service.RestaurantService;
import com.application.web.dto.post.NewAllergyDTO;
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
    private RestaurantRoleDAO restaurantRoleDAO;
    @Autowired
    private RestaurantPrivilegeDAO restaurantPrivilegeDAO;
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
    private AdminService adminService;
    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private AllergyService allergyService;
    @Autowired
    private RestaurantCategoryDAO restaurantCategoryDAO;

    static final Logger logger = LoggerFactory.getLogger(SetupDataLoader.class);

    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (!setupConfig.isAlreadySetup()) {
            logger.info(">>> --- Setup started --- <<<");
            setupConfig.setId(1L);
            setupConfig.setAlreadySetup(true);
            setupConfigDAO.save(setupConfig);
            createDefaultServiceTypes();
            adminSetup();
            customerSetup();
            createRestaurantPrivilegesAndRoles();
            logger.info(">>> --- Setup finished --- <<<");
        }
        if (!setupConfig.isDataUploaded()) {
            logger.info(">>> --- Creating Test data --- <<<");
            setupConfig.setAlreadySetup(true);
            setupConfig.setDataUploaded(true);
            createSomeAdmin();
            createSomeCustomer();
            createRestaurantLaSoffittaRenovatio();
            createAllergies();
            createRestaurantCategories();
            assignCategoriesToLaSoffittaRenovatio();
            createAdditionalRestaurants();
            logger.info("    >>>  ---   Test data Created   ---  <<< ");
        }
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

    @Transactional
    public void createDefaultServiceTypes() {
        logger.info(">>> --- Creating Default Service Types --- <<<");
        createServiceIfNotFound("Lunch");
        createServiceIfNotFound("Dinner");
        createServiceIfNotFound("Aperitif");
        createServiceIfNotFound("Breakfast");
        createServiceIfNotFound("After Dinner");
        logger.info(">>> --- Default Service Types Created --- <<<");
    }

    @Transactional
    private ServiceType createServiceIfNotFound(String name) {
        ServiceType serviceType = serviceTypeDAO.findByName(name);
        if (serviceType == null) {
            serviceType = new ServiceType(name);
            serviceTypeDAO.save(serviceType);
        }
        return serviceType;
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

    @Transactional
    private AdminPrivilege createAdminPrivilegeIfNotFound(final String name) {
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
        adminRoleDAO.save(role);
        return role;
    }

    @Transactional
    private void createRestaurantPrivilegesAndRoles() {
        logger.info(">>> --- Creating Restaurant Privileges and Roles --- <<<");
        final RestaurantPrivilege managerWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE");
        final RestaurantPrivilege chefWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_CHEF_WRITE");
        final RestaurantPrivilege waiterWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_WAITER_WRITE");
        final RestaurantPrivilege viewerWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_VIEWER_WRITE");
        final RestaurantPrivilege roleWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_ROLE_WRITE");
        final RestaurantPrivilege reservationWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE");
        final RestaurantPrivilege serviceWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE");
        final RestaurantPrivilege serviceReadPrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_SERVICE_READ");
        final RestaurantPrivilege slotWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_SLOT_WRITE");

        /* PRIVILEGE_VIEW_USERS PRIVILEGE_ADD_MANAGER PRIVILEGE_ADD_CHEF PRIVILEGE_ADD_WAITER PRIVILEGE_ADD_VIEWER PRIVILEGE_DISABLE_MANAGER PRIVILEGE_DISABLE_CHEF PRIVILEGE_DISABLE_WAITER PRIVILEGE_DISABLE_VIEWER PRIVILEGE_CHANGE_ROLE_TO_CHEF PRIVILEGE_CHANGE_ROLE_TO_WAITER PRIVILEGE_CHANGE_ROLE_TO_VIEWER PRIVILEGE_CHANGE_ROLE_TO_MANAGER PRIVILEGE_MODIFY_RESERVATION PRIVILEGE_CANCEL_RESERVATION PRIVILEGE_CHAT_WITH_CUSTOMERS PRIVILEGE_SERVICE_MANAGMENT */

        // Lista dei privilegi per i ruoli dei ristoranti
        final List<RestaurantPrivilege> ownerPrivileges = new ArrayList<>(Arrays.asList(
            managerWritePrivilege, chefWritePrivilege, waiterWritePrivilege, viewerWritePrivilege,
            roleWritePrivilege, reservationWritePrivilege, serviceWritePrivilege, serviceReadPrivilege,
            slotWritePrivilege));

        final List<RestaurantPrivilege> managerPrivileges = new ArrayList<>(Arrays.asList(
            chefWritePrivilege, waiterWritePrivilege, viewerWritePrivilege,
            roleWritePrivilege, reservationWritePrivilege, serviceWritePrivilege, serviceReadPrivilege,
            slotWritePrivilege));

        final List<RestaurantPrivilege> viewerPrivileges = new ArrayList<>(Arrays.asList(serviceReadPrivilege));
        final List<RestaurantPrivilege> chefPrivileges = new ArrayList<>(Arrays.asList(serviceReadPrivilege, slotWritePrivilege));
        final List<RestaurantPrivilege> waiterPrivileges = new ArrayList<>(Arrays.asList(serviceReadPrivilege));

        createRestaurantRoleIfNotFound("ROLE_OWNER", ownerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_MANAGER", managerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_VIEWER", viewerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_CHEF", chefPrivileges);
        createRestaurantRoleIfNotFound("ROLE_WAITER", waiterPrivileges);
        logger.info(">>> --- Restaurant Privileges and Roles Created --- <<<");
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
    public RestaurantRole createRestaurantRoleIfNotFound(String name, List<RestaurantPrivilege> privileges) {
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
    private void createSomeAdmin() {
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
        logger.info(">>> --- Creating Restaurant La Soffitta Renovatio --- <<<");
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

            restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
            restaurant.setStatus(Restaurant.Status.ENABLED);
            restaurantDAO.save(restaurant);

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
        for (Slot slot : slots) {
            slotDAO.save(slot);
        }
        return slots;
    }

    @Transactional
    private void createAllergies() {
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (setupConfig.isDataUploaded()) {
            logger.info(">>> --- Allergies already created, skipping --- <<<");
            return;
        }

        logger.info(">>> --- Creating Allergies --- <<<");
        List<NewAllergyDTO> allergies = Arrays.asList(
            new NewAllergyDTO("Cereals", "Includes wheat, rye, barley, oats, and foods like bread, pasta, and cereals."),
            new NewAllergyDTO("Shellfish", "Includes shrimp, crab, lobster, and other crustaceans."),
            new NewAllergyDTO("Eggs", "Includes eggs and foods containing eggs such as mayonnaise and baked goods."),
            new NewAllergyDTO("Fish", "Includes fish like salmon, tuna, and cod."),
            new NewAllergyDTO("Peanuts", "Includes peanuts and foods containing peanuts such as peanut butter."),
            new NewAllergyDTO("Soy", "Includes soybeans and soy-based products like tofu and soy milk."),
            new NewAllergyDTO("Milk", "Includes milk and dairy products like cheese, butter, and yogurt."),
            new NewAllergyDTO("Nuts", "Includes tree nuts like almonds, walnuts, and hazelnuts."),
            new NewAllergyDTO("Celery", "Includes celery and celery-based products like celery salt."),
            new NewAllergyDTO("Mustard", "Includes mustard seeds and mustard-based products."),
            new NewAllergyDTO("Sesame", "Includes sesame seeds and sesame oil."),
            new NewAllergyDTO("Sulfites", "Includes sulfites found in dried fruits, wine, and some processed foods."),
            new NewAllergyDTO("Lupin", "Includes lupin beans and lupin-based flour."),
            new NewAllergyDTO("Mollusks", "Includes clams, mussels, oysters, and squid."),
            new NewAllergyDTO("Gluten", "Includes foods containing gluten such as bread, pasta, and pastries."),
            new NewAllergyDTO("Corn", "Includes corn and corn-based products."),
            new NewAllergyDTO("Garlic", "Includes garlic and foods containing garlic."),
            new NewAllergyDTO("Onion", "Includes onion and foods containing onion."),
            new NewAllergyDTO("Pork", "Includes pork and pork-based products."),
            new NewAllergyDTO("Beef", "Includes beef and beef-based products."),
            new NewAllergyDTO("Chicken", "Includes chicken and chicken-based products."),
            new NewAllergyDTO("Alcohol", "Includes alcoholic beverages and foods containing alcohol."));

        for (NewAllergyDTO allergy : allergies) {
            if (allergyService.findByName(allergy.getName()) == null) {
                allergyService.createAllergy(allergy);
            }
        }

        setupConfig.setDataUploaded(true);
        setupConfigDAO.save(setupConfig);
        logger.info(">>> --- Allergies Created --- <<<");
    }

    @Transactional
    private void createRestaurantCategories() {
        logger.info(">>> --- Creating Restaurant Categories --- <<<");
        List<String> categories = Arrays.asList(
            "Pizzeria", "Cucina Italiana", "Cucina Romana", "Cinese", "Giapponese",
            "Sushi", "Senza Glutine", "Vegano", "Carne", "Pesce", "Fast Food",
            "Vegetariano", "Mediterranea", "Messicana", "Indiana", "Francese",
            "Spagnola", "Tailandese", "Coreana", "Barbecue", "Bisteccheria", "Griglieria",
            "Hamburgeria", "Birreria", "Pub", "Tapas", "Fusion", "Mediorientale",
            "Gourmet", "Creperia", "Pastificio", "Rosticceria", "Kebab", "Libanese",
            "Turca", "Greca", "Vietnamita", "Filippina", "Africana", "Peruviana",
            "Brasiliana", "Argentina", "Caraibica", "Hawaiiana", "Australiana",
            "Russa", "Tedesca", "Polacca", "Ungherese", "Nordica", "Casereccia",
            "Street Food", "Paninoteca", "Enoteca", "Churrascaria", "Dim Sum",
            "Tex-Mex", "Pizza al Taglio", "Trattoria", "Osteria", "Cioccolateria",
            "Gelateria", "Dessert Bar", "Tea House");

        for (String categoryName : categories) {
            if (restaurantCategoryDAO.findByName(categoryName) == null) {
                RestaurantCategory category = new RestaurantCategory();
                category.setName(categoryName);
                restaurantCategoryDAO.save(category);
            }
        }
        logger.info(">>> --- Restaurant Categories Created --- <<<");
    }

    @Transactional
    private void assignCategoriesToLaSoffittaRenovatio() {
        logger.info(">>> --- Assigning Categories to La Soffitta Renovatio --- <<<");
        Restaurant restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        if (restaurant == null) {
            logger.warn("Restaurant La Soffitta Renovatio not found.");
            return;
        }

        List<String> categoryNames = Arrays.asList(
            "Pizzeria", "Senza Glutine", "Vegano", "Cucina Italiana", 
            "Cucina Romana", "Carne", "Pesce");

        for (String categoryName : categoryNames) {
            RestaurantCategory category = restaurantCategoryDAO.findByName(categoryName);
            if (category != null && !restaurant.getRestaurantTypes().contains(category)) {
                restaurant.getRestaurantTypes().add(category);
            }
        }

        restaurantDAO.save(restaurant);
        logger.info("    >>>  ---   Categories Assigned to La Soffitta Renovatio   ---  <<< ");
    }

    @Transactional
    private void createAdditionalRestaurants() {
        logger.info("    >>>  ---   Creating Additional Restaurants   ---  <<< ");
        List<NewRestaurantDTO> restaurants = new ArrayList<>();
        restaurants.add(new NewRestaurantDTO("Ristorante Da Mario", "Via Roma 10", "info@damario.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Trattoria Bella Napoli", "Piazza Garibaldi 5", "info@bellanapoli.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Osteria La Pergola", "Via Dante 15", "info@lapergola.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Pizzeria Il Forno", "Corso Italia 20", "info@ilforno.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Ristorante Al Mare", "Lungomare 25", "info@almare.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Steakhouse La Griglia", "Via Veneto 30", "info@lagriglia.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Sushi Bar Tokyo", "Via Milano 40", "info@sushitokyo.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Ristorante Vegetariano Verde", "Via Firenze 50", "info@verde.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Ristorante Gourmet Stella", "Via Torino 60", "info@stellagourmet.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Ristorante Fusion Asia", "Via Napoli 70", "info@fusionasia.it", "Password123"));
        restaurants.add(new NewRestaurantDTO("Ristorante Prova", "Via Napoli 70", "info@lasoffittarenovatio.it", "Minosse100%%"));

        for (NewRestaurantDTO restaurantDto : restaurants) {
            if (restaurantDAO.findByName(restaurantDto.getName()) == null) {
                try {
                    restaurantService.registerRestaurant(restaurantDto);
                    Restaurant restaurant = restaurantDAO.findByName(restaurantDto.getName());
                    if (restaurant != null) {
                        restaurant.setStatus(Restaurant.Status.ENABLED);
                        restaurantDAO.save(restaurant);
                    }
                } catch (Exception e) {
                    logger.error("Error creating restaurant: " + restaurantDto.getName(), e);
                }
            }
        }
        logger.info("    >>>  ---   Additional Restaurants Created   ---  <<< ");
    }
}

