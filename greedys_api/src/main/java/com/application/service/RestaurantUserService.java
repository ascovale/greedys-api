package com.application.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantPrivilegeDAO;
import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.RestaurantUserVerificationTokenDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantPrivilege;
import com.application.persistence.model.restaurant.user.RestaurantRole;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserVerificationToken;
import com.application.persistence.model.user.Customer;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;

@Service
@Transactional
public class RestaurantUserService {

    public static final String TOKEN_INVALID = "invalidToken";
    public static final String TOKEN_EXPIRED = "expired";
    public static final String TOKEN_VALID = "valid";

    public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
    public static String APP_NAME = "SpringRegistration";

    @Autowired
    private RestaurantUserVerificationTokenDAO tokenDAO;

    @Autowired
    private EmailService emailService;
    @Autowired
    private RestaurantUserDAO ruDAO;
    @Autowired
    private CustomerDAO uDAO;
    @Autowired
    private RestaurantDAO restaurantDAO;
    @Autowired
    private RestaurantRoleDAO rrDAO;
    @Autowired
    private RestaurantPrivilegeDAO rpDAO;

    // TODO QUANDO CREO UN UTENTE DEVO SPECIFICARE IL RUOLO CHE HA NEL RISTORANTE
    // TODO Io farei che l'user può essere creato nello stesso tempo
    // se già esiste, lo prendo, altrimenti lo creo
    // invio di email come se fosse utente normale
    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " "
                + restaurantUserDTO.getUserId());
        RestaurantUser ru = new RestaurantUser();
        Restaurant restaurant = restaurantDAO.findById(restaurantUserDTO.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid restaurant ID: " + restaurantUserDTO.getRestaurantId()));
        ru.setRestaurant(restaurant);
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    public void acceptUser(Long id) {
        ruDAO.acceptUser(id);
    }

    public void blockRestaurantUser(Long idRestaurantUser) {
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));
        ru.setStatus(RestaurantUser.Status.BLOCKED);
        ruDAO.save(ru);
    }

    public void removeRestaurantUser(Long idRestaurantUser) {
        ruDAO.deleteById(idRestaurantUser);
    }

    @Transactional
    public void changeRestaurantOwner(Long idRestaurant, Long idOldOwner, Long idNewOwner) {
        // TODO
        // VERIFICARE CHE L'ID DEL Restaurant USer sia quello del restaurant corretto
        // cioè che esista un restaurantUser che abbia i permessi ROLE_OWNER e abbia
        // quel ristorante
        // basterebbe fare metodo findByIdandRestaurantId
        // e poi fare un controllo se il ru ha il ruolo ROLE_OWNER
        RestaurantUser oldOwner = ruDAO.findById(idOldOwner)
                .orElseThrow(() -> new IllegalArgumentException("Invalid old owner ID: " + idOldOwner));
        RestaurantUser newOwner = ruDAO.findById(idNewOwner)
                .orElseThrow(() -> new IllegalArgumentException("Invalid new owner ID: " + idNewOwner));
        new RestaurantRole("ROLE_OWNER");
        // TODO verificare quando deve essere creato il ruolo
        RestaurantRole ownerRole = rrDAO.findByName("ROLE_OWNER");

        if (ownerRole == null) {
            throw new IllegalArgumentException("Invalid role name: " + "ROLE_OWNER");
        }
        oldOwner.setStatus(RestaurantUser.Status.DELETED);
        newOwner.setStatus(RestaurantUser.Status.ENABLED);
        newOwner.addRole(new RestaurantRole("ROLE_OWNER"));
        oldOwner.removeRole(ownerRole);
        ruDAO.save(oldOwner);
        ruDAO.save(newOwner);

    }

    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " "
                + restaurantUserDTO.getUserId());
        RestaurantUser ru = new RestaurantUser();
        ru.setRestaurant(restaurant);
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    public RestaurantUserDTO addRestaurantUserRole(Long idRestaurantUser, Long idUser, String string) {
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));
        Restaurant restaurant = ru.getRestaurant();
        Customer user = uDAO.findById(idUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + idUser));

        if (ruDAO.findByRestaurantAndUser(restaurant, user).isPresent()) {
            throw new IllegalArgumentException("User already associated with this restaurant");
        }

        RestaurantUser newRestaurantUser = new RestaurantUser();
        newRestaurantUser.setRestaurant(restaurant);
        RestaurantRole role = rrDAO.findByName(string);
        if (role == null) {
            throw new IllegalArgumentException("Invalid role name: " + string);
        }
        // TODO verificare se è corretto
        newRestaurantUser.addRole(role);
        newRestaurantUser.addRole(new RestaurantRole(string));
        ruDAO.save(newRestaurantUser);

        return new RestaurantUserDTO(newRestaurantUser);
    }

    public void disableRestaurantUser(Long idRestaurantUser, Long idRestaurantUserToDisable) {
        RestaurantUser restaurantUserToDisable = ruDAO.findById(idRestaurantUserToDisable)
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUserToDisable));
        if (restaurantUserToDisable.hasRestaurantRole("ROLE_OWNER")) {
            throw new IllegalArgumentException("Cannot disable the owner of the restaurant");
        }
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));

        String roleNameWithoutPrefix = restaurantUserToDisable.getRoles().stream()
                .map(role -> role.getName().replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User does not have any roles"));
        boolean hasPermission = ru.getRoles().stream()
                .flatMap(role -> role.getPrivileges().stream())
                .anyMatch(privilege -> privilege.getName().equals("PRIVILEGE_DISABLE_" + roleNameWithoutPrefix));

        if (!hasPermission) {
            throw new IllegalArgumentException("User does not have permission to disable this role");
        }

        restaurantUserToDisable.setStatus(RestaurantUser.Status.DISABLED);
        ruDAO.save(restaurantUserToDisable);
    }

    public RestaurantUserDTO changeRestaurantUserRole(Long idRestaurantUser, Long idUser, String string) {
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));
        ru.addRole(new RestaurantRole(string));
        // non bisogna creare un nuovo ruolo bisogna mettere ruolo solo
        ruDAO.save(ru);
        return new RestaurantUserDTO(ru);
    }

    public void generateDefaultPrivilegesAndRoles() {
        String[] privilegeNames = {
                "PRIVILEGE_VIEW_USERS",
                "PRIVILEGE_ADD_MANAGER",
                "PRIVILEGE_ADD_CHEF",
                "PRIVILEGE_ADD_WAITER",
                "PRIVILEGE_ADD_VIEWER",
                "PRIVILEGE_DISABLE_MANAGER",
                "PRIVILEGE_DISABLE_CHEF",
                "PRIVILEGE_DISABLE_WAITER",
                "PRIVILEGE_DISABLE_VIEWER",
                "PRIVILEGE_CHANGE_ROLE_TO_CHEF",
                "PRIVILEGE_CHANGE_ROLE_TO_WAITER",
                "PRIVILEGE_CHANGE_ROLE_TO_VIEWER",
                "PRIVILEGE_CHANGE_ROLE_TO_MANAGER",
                "PRIVILEGE_ADD_RESERVATION",
                "PRIVILEGE_MODIFY_RESERVATION",
                "PRIVILEGE_CANCEL_RESERVATION",
                "PRIVILEGE_CHAT_WITH_CUSTOMERS"
        };

        String[] roleNames = {
                "ROLE_OWNER",
                "ROLE_MANAGER",
                "ROLE_CHEF",
                "ROLE_WAITER",
                "ROLE_VIEWER"
        };

        for (String roleName : roleNames) {
            if (rrDAO.findByName(roleName) == null) {
                RestaurantRole role = new RestaurantRole(roleName);
                rrDAO.save(role);
            }
        }
        Map<String, RestaurantPrivilege> privilegeMap = new HashMap<>();
        for (String privilegeName : privilegeNames) {
            RestaurantPrivilege privilege = rpDAO.findByName(privilegeName);
            if (privilege == null) {
                privilege = new RestaurantPrivilege(privilegeName);
                rpDAO.save(privilege);
            }
            privilegeMap.put(privilegeName, privilege);
        }

        // Associating privileges to roles
        RestaurantRole ownerRole = rrDAO.findByName("ROLE_OWNER");
        RestaurantRole managerRole = rrDAO.findByName("ROLE_MANAGER");
        RestaurantRole chefRole = rrDAO.findByName("ROLE_CHEF");
        RestaurantRole waiterRole = rrDAO.findByName("ROLE_WAITER");
        RestaurantRole viewerRole = rrDAO.findByName("ROLE_VIEWER");

        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_MANAGER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_CHEF"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_WAITER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_VIEWER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_MANAGER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_CHEF"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_WAITER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_VIEWER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_CHEF"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_WAITER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_VIEWER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_MANAGER"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));
        ownerRole.addPrivilege(privilegeMap.get("PRIVILEGE_GESTIONE_SERVIZI"));

        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_CHEF"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_WAITER"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_VIEWER"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_CHEF"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_WAITER"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_VIEWER"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_CHEF"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_WAITER"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_VIEWER"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));
        managerRole.addPrivilege(privilegeMap.get("PRIVILEGE_GESTIONE_SERVIZI"));

        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_WAITER"));
        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_VIEWER"));
        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        chefRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));

        waiterRole.addPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        waiterRole.addPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        waiterRole.addPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        waiterRole.addPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        waiterRole.addPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));

        viewerRole.addPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));

        rrDAO.save(ownerRole);
        rrDAO.save(managerRole);
        rrDAO.save(chefRole);
        rrDAO.save(waiterRole);
        rrDAO.save(viewerRole);
    }

    public String validateVerificationToken(String token) {
        final RestaurantUserVerificationToken verificationToken = tokenDAO.findByToken(token);
        if (verificationToken == null) {
            return TOKEN_INVALID;
        }

        final RestaurantUser user = verificationToken.getUser();
        final LocalDateTime now = LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            tokenDAO.delete(verificationToken);
            return TOKEN_EXPIRED;
        }

        user.setEnabled(true);
        // tokenDAO.delete(verificationToken);
        ruDAO.save(user);
        return TOKEN_VALID;
    }

    public RestaurantUser getRestaurantUser(final String verificationToken) {
		final RestaurantUserVerificationToken token = tokenDAO.findByToken(verificationToken);
		if (token != null) {
			return token.getUser();
		}
		return null;
	}

    public RestaurantUserVerificationToken generateNewVerificationToken(final String existingVerificationToken) {
		RestaurantUserVerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
		vToken.updateToken(UUID.randomUUID().toString());
		vToken = tokenDAO.save(vToken);
		return vToken;
    }
}