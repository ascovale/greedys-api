package com.application.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.application.security.user.restaurant.RestaurantUserDetailsService;
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

    private RestaurantUserVerificationTokenDAO tokenDAO;
    private RestaurantUserDetailsService userDetailsService;
    private EmailService emailService;
    private RestaurantUserDAO ruDAO;
    private RestaurantDAO restaurantDAO;
    private RestaurantRoleDAO rrDAO;
    private RestaurantPrivilegeDAO rpDAO;
    private PasswordEncoder passwordEncoder;

    public RestaurantUserService(
            RestaurantUserVerificationTokenDAO tokenDAO,
            RestaurantUserDetailsService userDetailsService,
            EmailService emailService,
            RestaurantUserDAO ruDAO,
            RestaurantDAO restaurantDAO,
            RestaurantRoleDAO rrDAO,
            RestaurantPrivilegeDAO rpDAO,
            PasswordEncoder passwordEncoder) {
        this.tokenDAO = tokenDAO;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
        this.ruDAO = ruDAO;
        this.restaurantDAO = restaurantDAO;
        this.rrDAO = rrDAO;
        this.rpDAO = rpDAO;
        this.passwordEncoder = passwordEncoder;
    }

    // TODO QUANDO CREO UN UTENTE DEVO SPECIFICARE IL RUOLO CHE HA NEL RISTORANTE
    // TODO Io farei che l'user può essere creato nello stesso tempo
    // se già esiste, lo prendo, altrimenti lo creo
    // invio di email come se fosse utente normale

    public void acceptRestaurantUser(Long id) {
        ruDAO.acceptRestaurantUser(id);
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
        Hibernate.initialize(newOwner.getRestaurantRoles());
        newOwner.addRestaurantRole(new RestaurantRole("ROLE_OWNER"));
        
        oldOwner.removeRole(ownerRole);
        ruDAO.save(oldOwner);
        ruDAO.save(newOwner);

    }

    
    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant, RestaurantRole rr) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " ");
        RestaurantUser ru = new RestaurantUser();
        Hibernate.initialize(ru.getRestaurantRoles());
        ru.addRestaurantRole(rr);
        ru.setRestaurant(restaurant);
        ru.setPassword(passwordEncoder.encode(restaurantUserDTO.getPassword()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " ");
        RestaurantUser ru = new RestaurantUser();
        Hibernate.initialize(ru.getRestaurantRoles());
        ru.setRestaurant(restaurant);
        ru.setPassword(passwordEncoder.encode(restaurantUserDTO.getPassword()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    //Add restaurant user bisogna verificare che il ristorante esista

    public RestaurantUserDTO addRestaurantUserRole(Long idRestaurantUser, String string) {
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));
        Restaurant restaurant = ru.getRestaurant();
        if (ru.getRestaurantRoles().stream().anyMatch(role -> role.getName().equals(string))) {
            throw new IllegalArgumentException("User already has the role: " + string);
        }
        RestaurantRole role = rrDAO.findByName(string);
        Hibernate.initialize(ru.getRestaurantRoles());
        ru.addRestaurantRole(role);

        RestaurantUser newRestaurantUser = new RestaurantUser();
        newRestaurantUser.setRestaurant(restaurant);
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

        String roleNameWithoutPrefix = restaurantUserToDisable.getRestaurantRoles().stream()
                .map(role -> role.getName().replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User does not have any roles"));
        boolean hasPermission = ru.getRestaurantRoles().stream()
                .flatMap(role -> role.getRestaurantPrivileges().stream())
                .anyMatch(privilege -> privilege.getName().equals("PRIVILEGE_DISABLE_" + roleNameWithoutPrefix));

        if (!hasPermission) {
            throw new IllegalArgumentException("User does not have permission to disable this role");
        }

        restaurantUserToDisable.setStatus(RestaurantUser.Status.DISABLED);
        ruDAO.save(restaurantUserToDisable);
    }

    public RestaurantUserDTO changeRestaurantUserRole(Long idRestaurantUser, Long idUser, String string) {
        //TODO implementare una logica che capisca se il primo user ha ruolo per cambiare il ruolo del secondo
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));
        RestaurantRole role = rrDAO.findByName(string);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + string);
        }
        if (ru.getRestaurantRoles().stream().anyMatch(r -> r.getName().equals("ROLE_OWNER"))) {
            throw new IllegalArgumentException("Cannot change role for a user with ROLE_OWNER");
        }
        ru.getRestaurantRoles().clear();
        Hibernate.initialize(ru.getRestaurantRoles());
        ru.addRestaurantRole(role);
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
        //TODO: MANCA IL METODO AGGIUNGI PRIVILEGIO E TOGLIO PRIVILEGIO A RUOLO

        // Associating privileges to roles
        RestaurantRole ownerRole = rrDAO.findByName("ROLE_OWNER");
        RestaurantRole managerRole = rrDAO.findByName("ROLE_MANAGER");
        RestaurantRole chefRole = rrDAO.findByName("ROLE_CHEF");
        RestaurantRole waiterRole = rrDAO.findByName("ROLE_WAITER");
        RestaurantRole viewerRole = rrDAO.findByName("ROLE_VIEWER");
        Hibernate.initialize(ownerRole.getRestaurantPrivileges());
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_MANAGER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_CHEF"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_WAITER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_VIEWER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_MANAGER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_CHEF"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_WAITER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_VIEWER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_CHEF"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_WAITER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_VIEWER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_MANAGER"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));
        ownerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_GESTIONE_SERVIZI"));

        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_CHEF"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_WAITER"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_VIEWER"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_CHEF"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_WAITER"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_DISABLE_VIEWER"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_CHEF"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_WAITER"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_VIEWER"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));
        managerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_GESTIONE_SERVIZI"));

        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_WAITER"));
        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHANGE_ROLE_TO_VIEWER"));
        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        chefRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));

        waiterRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));
        waiterRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_ADD_RESERVATION"));
        waiterRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_MODIFY_RESERVATION"));
        waiterRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CANCEL_RESERVATION"));
        waiterRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_CHAT_WITH_CUSTOMERS"));

        viewerRole.addRestaurantPrivilege(privilegeMap.get("PRIVILEGE_VIEW_USERS"));

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

        final RestaurantUser user = verificationToken.getRestaurantUser();
        final LocalDateTime now = LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            tokenDAO.delete(verificationToken);
            return TOKEN_EXPIRED;
        }

        user.setStatus(RestaurantUser.Status.ENABLED);
        // tokenDAO.delete(verificationToken);
        ruDAO.save(user);
        return TOKEN_VALID;
    }

    public RestaurantUser getRestaurantUser(final String verificationToken) {
        final RestaurantUserVerificationToken token = tokenDAO.findByToken(verificationToken);
        if (token != null) {
            return token.getRestaurantUser();
        }
        return null;
    }

    public RestaurantUserVerificationToken generateNewVerificationToken(final String existingVerificationToken) {
        RestaurantUserVerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
        vToken.updateToken(UUID.randomUUID().toString());
        vToken = tokenDAO.save(vToken);
        return vToken;
    }

    public void switchToRestaurantUser(Long restaurantUserId) {
        UserDetails userDetails = userDetailsService.loadUserById(restaurantUserId);
        if (userDetails == null) {
            throw new UsernameNotFoundException("No user found with username: " + restaurantUserId.toString());
        }
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, userDetails.getPassword(), userDetails.getAuthorities()));
    }

    public void disconnectRestaurantUser(Long restaurantUserId) {
        UserDetails userDetails = userDetailsService.loadSwitchUserById(restaurantUserId);
        if (userDetails == null) {
            throw new UsernameNotFoundException("No user found with username: " + restaurantUserId.toString());
        }
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, userDetails.getPassword(), userDetails.getAuthorities()));
    }

    public RestaurantUser findRestaurantUserByEmail(String userEmail) {
        return ruDAO.findByEmail(userEmail);
    }

    public void updateRestaurantUserStatus(Long restaurantUserId, RestaurantUser.Status newStatus) {
        RestaurantUser ru = ruDAO.findById(restaurantUserId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Aggiorna lo stato del customer
        ru.setStatus(newStatus);
        ruDAO.save(ru);

        // rifare jwt
    }
}