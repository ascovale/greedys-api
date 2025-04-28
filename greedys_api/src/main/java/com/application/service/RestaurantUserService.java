package com.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantPrivilegeDAO;
import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.RestaurantUserHubDAO;
import com.application.persistence.dao.restaurant.RestaurantUserPasswordResetTokenDAO;
import com.application.persistence.dao.restaurant.RestaurantUserVerificationTokenDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RestaurantRole;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.persistence.model.restaurant.user.RestaurantUserHub;
import com.application.persistence.model.restaurant.user.RestaurantUserPasswordResetToken;
import com.application.persistence.model.restaurant.user.RestaurantUserVerificationToken;
import com.application.security.jwt.JwtUtil;
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
    private EmailService emailService;
    private RestaurantUserDAO ruDAO;
    private RestaurantDAO restaurantDAO;
    private RestaurantRoleDAO rrDAO;
    private RestaurantUserHubDAO ruhDAO;
    private PasswordEncoder passwordEncoder;
    private final RestaurantUserPasswordResetTokenDAO passwordTokenRepository;

    public RestaurantUserService(
            RestaurantUserVerificationTokenDAO tokenDAO,
            RestaurantUserDetailsService userDetailsService,
            EmailService emailService,
            RestaurantUserDAO ruDAO,
            RestaurantDAO restaurantDAO,
            RestaurantRoleDAO rrDAO,
            RestaurantPrivilegeDAO rpDAO,
            PasswordEncoder passwordEncoder,
            RestaurantUserHubDAO ruhDAO,
            RestaurantUserPasswordResetTokenDAO passwordTokenRepository,
            @Qualifier("restaurantAuthenticationManager") AuthenticationManager authenticationManager,
            JwtUtil jwtUtil) {
        this.tokenDAO = tokenDAO;
        this.emailService = emailService;
        this.ruDAO = ruDAO;
        this.restaurantDAO = restaurantDAO;
        this.rrDAO = rrDAO;
        this.passwordEncoder = passwordEncoder;
        this.passwordTokenRepository = passwordTokenRepository;
        this.ruhDAO = ruhDAO;
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

    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant,
            RestaurantRole rr) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " ");

        // Check if a user with the same email already exists
        RestaurantUserHub existingUserHub = ruhDAO.findByEmail(restaurantUserDTO.getEmail());
        if (existingUserHub != null) {
            throw new IllegalArgumentException("User hub with email " + restaurantUserDTO.getEmail() + " already exists.");
        }
        
        RestaurantUser existingRestaurantUser = ruDAO.findByEmailAndRestaurantId(restaurantUserDTO.getEmail(), restaurant.getId());
        if (existingRestaurantUser != null) {
            throw new IllegalArgumentException("User already exists for this restaurant.");
        }

        RestaurantUser ru = new RestaurantUser();
        RestaurantUserHub restaurantUserHub = new RestaurantUserHub();
        restaurantUserHub.setEmail(restaurantUserDTO.getEmail());
        restaurantUserHub.setFirstName(restaurantUserDTO.getFirstName());
        restaurantUserHub.setLastName(restaurantUserDTO.getLastName());
        
        // Save the RestaurantUserHub entity before associating it with RestaurantUser
        ruhDAO.save(restaurantUserHub);

        ru.setRestaurantUserHub(restaurantUserHub);
        Hibernate.initialize(ru.getRestaurantRoles());
        ru.addRestaurantRole(rr);
        ru.setRestaurant(restaurant);
        restaurantUserHub.setPassword(passwordEncoder.encode(restaurantUserDTO.getPassword()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " ");

        // Check if a user with the same email already exists
        RestaurantUserHub existingUserHub = ruhDAO.findByEmail(restaurantUserDTO.getEmail());
        if (existingUserHub != null) {
            throw new IllegalArgumentException("User hub with email " + restaurantUserDTO.getEmail() + " already exists.");
        }

        RestaurantUser existingRestaurantUser = ruDAO.findByEmailAndRestaurantId(restaurantUserDTO.getEmail(), restaurant.getId());
        if (existingRestaurantUser != null) {
            throw new IllegalArgumentException("User already exists for this restaurant.");
        }

        RestaurantUser ru = new RestaurantUser();
        RestaurantUserHub restaurantUserHub = new RestaurantUserHub();
        restaurantUserHub.setEmail(restaurantUserDTO.getEmail());
        restaurantUserHub.setFirstName(restaurantUserDTO.getFirstName());
        restaurantUserHub.setLastName(restaurantUserDTO.getLastName());
        
        // Save the RestaurantUserHub entity before associating it with RestaurantUser
        ruhDAO.save(restaurantUserHub);

        ru.setRestaurantUserHub(restaurantUserHub);
        Hibernate.initialize(ru.getRestaurantRoles());
        ru.setRestaurant(restaurant);
        restaurantUserHub.setPassword(passwordEncoder.encode(restaurantUserDTO.getPassword()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    public RestaurantUser addRestaurantUserIfHubExists(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant) {
        // Check if a user hub with the same email already exists
        RestaurantUserHub existingUserHub = ruhDAO.findByEmail(restaurantUserDTO.getEmail());
        if (existingUserHub == null) {
            throw new IllegalArgumentException("No user hub found with email: " + restaurantUserDTO.getEmail());
        }

        // Check if a RestaurantUser already exists for this hub and restaurant
        RestaurantUser existingRestaurantUser = ruDAO.findByEmailAndRestaurantId(restaurantUserDTO.getEmail(), restaurant.getId());
        if (existingRestaurantUser != null) {
            throw new IllegalArgumentException("User already exists for this restaurant.");
        }

        // Create and save a new RestaurantUser
        RestaurantUser ru = new RestaurantUser();
        ru.setRestaurantUserHub(existingUserHub);
        ru.setRestaurant(restaurant);
        Hibernate.initialize(ru.getRestaurantRoles());
        ruDAO.save(ru);

        return ru;
    }

    // Add restaurant user bisogna verificare che il ristorante esista

    public RestaurantUserDTO addRestaurantUserRole(Long idRestaurantUser, String string) {
        if ("ROLE_OWNER".equals(string)) {
            throw new IllegalArgumentException("Cannot add the ROLE_OWNER role to a user.");
        }

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
        // TODO implementare una logica che capisca se il primo user ha ruolo per
        // cambiare il ruolo del secondo
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

    public boolean checkIfValidOldPassword(final Long id, final String oldPassword) {
        return passwordEncoder.matches(oldPassword, ruDAO.findById(id).get().getPassword());
    }

    public void changeRestaurantUserPassword(final Long id, final String password) {
        final RestaurantUser ru = ruDAO.findById(id).get();
        ru.getRestaurantUserHub().setPassword(passwordEncoder.encode(password));
        ruDAO.save(ru);
    }

    public void createPasswordResetTokenForRestaurantUser(final RestaurantUser ru, final String token) {
        final RestaurantUserPasswordResetToken myToken = new RestaurantUserPasswordResetToken(token, ru);
        passwordTokenRepository.save(myToken);
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Transactional
    public RestaurantUserDTO addRestaurantUserToRestaurant(NewRestaurantUserDTO restaurantUserDTO, Long restaurantId) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID: " + restaurantId));
        RestaurantUser restaurantUser = registerRestaurantUser(restaurantUserDTO, restaurant);

        // Generate and send verification token
        String token = UUID.randomUUID().toString();
        createVerificationTokenForRestaurantUser(restaurantUser, token);
        // TODO: Verificare invio mail
        // final SimpleMailMessage email = constructEmailMessage(event, restaurantUser,
        // token);
        // mailSender.send(email);

        return new RestaurantUserDTO(restaurantUser);
    }

    @Transactional
    public RestaurantUserDTO addRestaurantUserToRestaurantWithRole(NewRestaurantUserDTO restaurantUserDTO,
            Long restaurantId, String roleName) {
        if ("ROLE_OWNER".equals(roleName)) {
            throw new IllegalArgumentException("Cannot assign the ROLE_OWNER role to a user.");
        }

        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID: " + restaurantId));
        RestaurantRole role = rrDAO.findByName(roleName);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + roleName);
        }
        RestaurantUser restaurantUser = registerRestaurantUser(restaurantUserDTO, restaurant, role);

        // Generate and send verification token
        String token = UUID.randomUUID().toString();
        createVerificationTokenForRestaurantUser(restaurantUser, token);
        // TODO: Verificare invio mail
        // final SimpleMailMessage email = constructEmailMessage(event, restaurantUser,
        // token);
        // mailSender.send(email);

        return new RestaurantUserDTO(restaurantUser);
    }

    public void createVerificationTokenForRestaurantUser(final RestaurantUser user, final String token) {
        final RestaurantUserVerificationToken myToken = new RestaurantUserVerificationToken(token, user);
        tokenDAO.save(myToken);
    }

    public RestaurantUserDTO removeRestaurantUserRole(Long restaurantUserId, String string) {
        if ("ROLE_OWNER".equals(string)) {
            throw new IllegalArgumentException("Cannot remove the ROLE_OWNER role from a user.");
        }

        RestaurantUser ru = ruDAO.findById(restaurantUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + restaurantUserId));

        RestaurantRole role = rrDAO.findByName(string);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + string);
        }

        if (ru.getRestaurantRoles().stream().noneMatch(r -> r.getName().equals(string))) {
            throw new IllegalArgumentException("User does not have the role: " + string);
        }

        Hibernate.initialize(ru.getRestaurantRoles());
        ru.removeRole(role);
        ruDAO.save(ru);

        return new RestaurantUserDTO(ru);
    }

    public List<RestaurantUserDTO> getRestaurantUsersByRestaurantId(Long restaurantId) {
        return ruDAO.findByRestaurantId(restaurantId).stream()
            .map(RestaurantUserDTO::new)
            .toList();
    }

}