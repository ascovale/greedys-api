package com.application.restaurant;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.ServiceType;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.service.RestaurantService;
import com.application.restaurant.persistence.dao.RestaurantCategoryDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RestaurantPrivilegeDAO;
import com.application.restaurant.persistence.dao.RestaurantRoleDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.ServiceTypeDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantCategory;
import com.application.restaurant.persistence.model.user.RestaurantPrivilege;
import com.application.restaurant.persistence.model.user.RestaurantRole;
import com.application.restaurant.web.dto.restaurant.NewRestaurantDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantDataLoader {

    private final RestaurantDAO restaurantDAO;
    private final ServiceTypeDAO serviceTypeDAO;
    private final ServiceDAO serviceDAO;
    private final SlotDAO slotDAO;
    private final RestaurantService restaurantService;
    private final RestaurantPrivilegeDAO restaurantPrivilegeDAO;
    private final RestaurantRoleDAO restaurantRoleDAO;
    private final RestaurantCategoryDAO restaurantCategoryDAO;

    @Transactional
    public void createRestaurantLaSoffittaRenovatio() {
        log.info(">>> --- Creating Restaurant La Soffitta Renovatio --- <<<");
        Restaurant restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        if (restaurant != null) {
            log.info("Restaurant with name La Soffitta Renovatio already exists.");
            return;
        }

        NewRestaurantDTO restaurantDto = NewRestaurantDTO.builder()
            .name("La Soffitta Renovatio")
            .address("Piazza del Risorgimento 46A")
            .email("info@lasoffittarenovatio.it")
            .password("Minosse100%")
            .ownerName("Luca")
            .ownerSurname("Bianchi")
            .build();
        restaurantService.registerRestaurant(restaurantDto);

        restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        restaurant.setStatus(Restaurant.Status.ENABLED);
        restaurantDAO.save(restaurant);

        ServiceType pranzoType = serviceTypeDAO.findByName("Lunch");
        Service pranzo = Service.builder()
            .validFrom(LocalDate.now())
            .validTo(LocalDate.now())
            .restaurant(restaurant)
            .build();
        pranzo.addServiceType(pranzoType);
        serviceDAO.save(pranzo);
        createSlotsForService(pranzo, LocalTime.of(11, 0), LocalTime.of(17, 0));

        ServiceType cenaType = serviceTypeDAO.findByName("Dinner");
        Service cena = Service.builder()
            .validFrom(LocalDate.now())
            .validTo(LocalDate.now())
            .restaurant(restaurant)
            .build();
        cena.addServiceType(cenaType);
        serviceDAO.save(cena);
        createSlotsForService(cena, LocalTime.of(17, 30), LocalTime.of(23, 0));
    }

    @Transactional
    public void createRestaurantTrattoriaDaMario() {
        log.info(">>> --- Creating Restaurant Trattoria Da Mario --- <<<");
        Restaurant restaurant = restaurantDAO.findByName("Trattoria Da Mario");
        if (restaurant != null) {
            log.info("Restaurant with name Trattoria Da Mario already exists.");
            return;
        }

        NewRestaurantDTO restaurantDto = NewRestaurantDTO.builder()
            .name("Trattoria Da Mario")
            .address("Piazza del Risorgimento 46A")
            .email("info@damario.it") 
            .password("Mario123%")
            .ownerName("Mario")
            .ownerSurname("Rossi")
            .build();
        restaurantService.registerRestaurant(restaurantDto);

        restaurant = restaurantDAO.findByName("Trattoria Da Mario");
        restaurant.setStatus(Restaurant.Status.ENABLED);
        restaurantDAO.save(restaurant);

        ServiceType pranzoType = serviceTypeDAO.findByName("Lunch");
        Service pranzo = Service.builder()
            .validFrom(LocalDate.now())
            .validTo(LocalDate.now())
            .restaurant(restaurant)
            .build();
        pranzo.addServiceType(pranzoType);
        serviceDAO.save(pranzo);
        createSlotsForService(pranzo, LocalTime.of(11, 0), LocalTime.of(17, 0));

        ServiceType cenaType = serviceTypeDAO.findByName("Dinner");
        Service cena = Service.builder()
            .validFrom(LocalDate.now())
            .validTo(LocalDate.now())
            .restaurant(restaurant)
            .build();
        cena.addServiceType(cenaType);
        serviceDAO.save(cena);
        createSlotsForService(cena, LocalTime.of(17, 30), LocalTime.of(23, 0));
    }

    private List<Slot> createSlotsForService(Service service, LocalTime startTime, LocalTime endTime) {
        List<Slot> slots = new java.util.ArrayList<>();
        LocalTime time = startTime;
        while (time.isBefore(endTime)) {
            for (int day = 1; day <= 7; day++) {
                Weekday weekday = Weekday.values()[day - 1];
                Slot slot = Slot.builder()
                    .start(time)
                    .end(time.plusMinutes(30))
                    .service(service)
                    .weekday(weekday)
                    .build();
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
    private RestaurantPrivilege createRestaurantPrivilegeIfNotFound(final String name) {
        RestaurantPrivilege privilege = restaurantPrivilegeDAO.findByName(name);
        if (privilege == null) {
            privilege = RestaurantPrivilege.builder()
                    .name(name)
                    .build();
            privilege = restaurantPrivilegeDAO.save(privilege);
        }
        return privilege;
    }

    @Transactional
    public RestaurantRole createRestaurantRoleIfNotFound(String name, List<RestaurantPrivilege> privileges) {
        RestaurantRole role = restaurantRoleDAO.findByName(name);
        if (role == null) {
            role = new RestaurantRole();
            role.setName(name);
        }
        // Ensure privileges is always a mutable list
        role.setPrivileges(new ArrayList<>(privileges));
        role = restaurantRoleDAO.save(role);
        return role;
    }

    @Transactional
    public void createRestaurantPrivilegesAndRoles() {
        log.info(">>> --- Creating Restaurant Privileges and Roles --- <<<");
        final RestaurantPrivilege managerWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_MANAGER_WRITE");
        final RestaurantPrivilege chefWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_CHEF_WRITE");
        final RestaurantPrivilege waiterWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_WAITER_WRITE");
        final RestaurantPrivilege viewerWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_VIEWER_WRITE");
        final RestaurantPrivilege roleWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_ROLE_WRITE");
        final RestaurantPrivilege reservationWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_RESERVATION_WRITE");
        final RestaurantPrivilege serviceWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_SERVICE_WRITE");
        final RestaurantPrivilege serviceReadPrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_SERVICE_READ");
        final RestaurantPrivilege slotWritePrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_RESTAURANT_USER_SLOT_WRITE");
        final RestaurantPrivilege changePasswordPrivilege = createRestaurantPrivilegeIfNotFound("PRIVILEGE_CHANGE_PASSWORD");

        /* PRIVILEGE_VIEW_USERS PRIVILEGE_ADD_MANAGER PRIVILEGE_ADD_CHEF PRIVILEGE_ADD_WAITER PRIVILEGE_ADD_VIEWER PRIVILEGE_DISABLE_MANAGER PRIVILEGE_DISABLE_CHEF PRIVILEGE_DISABLE_WAITER PRIVILEGE_DISABLE_VIEWER PRIVILEGE_CHANGE_ROLE_TO_CHEF PRIVILEGE_CHANGE_ROLE_TO_WAITER PRIVILEGE_CHANGE_ROLE_TO_VIEWER PRIVILEGE_CHANGE_ROLE_TO_MANAGER PRIVILEGE_MODIFY_RESERVATION PRIVILEGE_CANCEL_RESERVATION PRIVILEGE_CHAT_WITH_CUSTOMERS PRIVILEGE_SERVICE_MANAGMENT */

        // Lista dei privilegi per i ruoli dei ristoranti
        final List<RestaurantPrivilege> ownerPrivileges = new ArrayList<>(Arrays.asList(
            managerWritePrivilege, chefWritePrivilege, waiterWritePrivilege, viewerWritePrivilege,
            roleWritePrivilege, reservationWritePrivilege, serviceWritePrivilege, serviceReadPrivilege,
            slotWritePrivilege, changePasswordPrivilege));

        final List<RestaurantPrivilege> managerPrivileges = new ArrayList<>(Arrays.asList(
            chefWritePrivilege, waiterWritePrivilege, viewerWritePrivilege,
            roleWritePrivilege, reservationWritePrivilege, serviceWritePrivilege, serviceReadPrivilege,
            slotWritePrivilege, changePasswordPrivilege));

        final List<RestaurantPrivilege> viewerPrivileges = new ArrayList<>(Arrays.asList(serviceReadPrivilege, changePasswordPrivilege));
        final List<RestaurantPrivilege> chefPrivileges = new ArrayList<>(Arrays.asList(serviceReadPrivilege, slotWritePrivilege, changePasswordPrivilege));
        final List<RestaurantPrivilege> waiterPrivileges = new ArrayList<>(Arrays.asList(serviceReadPrivilege, changePasswordPrivilege));

        createRestaurantRoleIfNotFound("ROLE_OWNER", ownerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_MANAGER", managerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_VIEWER", viewerPrivileges);
        createRestaurantRoleIfNotFound("ROLE_CHEF", chefPrivileges);
        createRestaurantRoleIfNotFound("ROLE_WAITER", waiterPrivileges);
        log.info(">>> --- Restaurant Privileges and Roles Created --- <<<");
    }

    @Transactional
    public void createDefaultServiceTypes() {
        log.info(">>> --- Creating Default Service Types --- <<<");
        createServiceIfNotFound("Lunch");
        createServiceIfNotFound("Dinner");
        createServiceIfNotFound("Aperitif");
        createServiceIfNotFound("Breakfast");
        createServiceIfNotFound("After Dinner");
        log.info(">>> --- Default Service Types Created --- <<<");
    }

    @Transactional
    private ServiceType createServiceIfNotFound(String name) {
        ServiceType serviceType = serviceTypeDAO.findByName(name);
        if (serviceType == null) {
            serviceType = ServiceType.builder()
                .name(name)
                .build();
            serviceTypeDAO.save(serviceType);
        }
        return serviceType;
    }

    
    @Transactional
    public void createRestaurantCategories() {
        log.info(">>> --- Creating Restaurant Categories --- <<<");
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
                RestaurantCategory category = RestaurantCategory.builder()
                        .name(categoryName)
                        .build();
                restaurantCategoryDAO.save(category);
            }
        }
        log.info(">>> --- Restaurant Categories Created --- <<<");
    }

    @Transactional
    public void assignCategoriesToLaSoffittaRenovatio() {
        log.info(">>> --- Assigning Categories to La Soffitta Renovatio --- <<<");
        Restaurant restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        if (restaurant == null) {
            log.warn("Restaurant La Soffitta Renovatio not found.");
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
        log.info("    >>>  ---   Categories Assigned to La Soffitta Renovatio   ---  <<< ");
    }
    
   
}
