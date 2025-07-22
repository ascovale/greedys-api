package com.application.restaurant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.jwt.JwtUtil;
import com.application.common.service.EmailService;
import com.application.common.web.dto.get.RUserDTO;
import com.application.restaurant.dao.RUserDAO;
import com.application.restaurant.dao.RUserHubDAO;
import com.application.restaurant.dao.RUserPasswordResetTokenDAO;
import com.application.restaurant.dao.RUserVerificationTokenDAO;
import com.application.restaurant.dao.RestaurantDAO;
import com.application.restaurant.dao.RestaurantPrivilegeDAO;
import com.application.restaurant.dao.RestaurantRoleDAO;
import com.application.restaurant.model.Restaurant;
import com.application.restaurant.model.user.RUser;
import com.application.restaurant.model.user.RUserHub;
import com.application.restaurant.model.user.RUserPasswordResetToken;
import com.application.restaurant.model.user.RUserVerificationToken;
import com.application.restaurant.model.user.RestaurantRole;
import com.application.restaurant.service.security.RUserDetailsService;
import com.application.restaurant.web.post.NewRUserDTO;

@Service
@Transactional
public class RUserService {

    public static final String TOKEN_INVALID = "invalidToken";
    public static final String TOKEN_EXPIRED = "expired";
    public static final String TOKEN_VALID = "valid";

    public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
    public static String APP_NAME = "SpringRegistration";

    private RUserVerificationTokenDAO tokenDAO;
    private EmailService emailService;
    private RUserDAO ruDAO;
    private RestaurantDAO restaurantDAO;
    private RestaurantRoleDAO rrDAO;
    private RUserHubDAO ruhDAO;
    private PasswordEncoder passwordEncoder;
    private final RUserPasswordResetTokenDAO passwordTokenRepository;

    public RUserService(
            RUserVerificationTokenDAO tokenDAO,
            RUserDetailsService userDetailsService,
            EmailService emailService,
            RUserDAO ruDAO,
            RestaurantDAO restaurantDAO,
            RestaurantRoleDAO rrDAO,
            RestaurantPrivilegeDAO rpDAO,
            PasswordEncoder passwordEncoder,
            RUserHubDAO ruhDAO,
            RUserPasswordResetTokenDAO passwordTokenRepository,
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

    public void acceptRUser(Long id) {
        ruDAO.setUserStatus(id, RUser.Status.ENABLED);
    }

    public void blockRUser(Long idRUser) {
        RUser ru = ruDAO.findById(idRUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRUser));
        ru.setStatus(RUser.Status.BLOCKED);
        ruDAO.save(ru);
    }

    public void removeRUser(Long idRUser) {
        ruDAO.deleteById(idRUser);
    }

    public void changeRestaurantOwner(Long idRestaurant, Long idOldOwner, Long idNewOwner) {
        // TODO
        // VERIFICARE CHE L'ID DEL Restaurant USer sia quello del restaurant corretto
        // cioè che esista un RUser che abbia i permessi ROLE_OWNER e abbia
        // quel ristorante
        // basterebbe fare metodo findByIdandRestaurantId
        // e poi fare un controllo se il ru ha il ruolo ROLE_OWNER
        RUser oldOwner = ruDAO.findById(idOldOwner)
                .orElseThrow(() -> new IllegalArgumentException("Invalid old owner ID: " + idOldOwner));
        RUser newOwner = ruDAO.findById(idNewOwner)
                .orElseThrow(() -> new IllegalArgumentException("Invalid new owner ID: " + idNewOwner));
        new RestaurantRole("ROLE_OWNER");
        // TODO verificare quando deve essere creato il ruolo
        RestaurantRole ownerRole = rrDAO.findByName("ROLE_OWNER");

        if (ownerRole == null) {
            throw new IllegalArgumentException("Invalid role name: " + "ROLE_OWNER");
        }
        oldOwner.setStatus(RUser.Status.DELETED);
        newOwner.setStatus(RUser.Status.ENABLED);
        Hibernate.initialize(newOwner.getRoles());
        newOwner.addRestaurantRole(new RestaurantRole("ROLE_OWNER"));

        oldOwner.removeRole(ownerRole);
        ruDAO.save(oldOwner);
        ruDAO.save(newOwner);

    }

    public RUserHub registerHub(RUserHub userHub) {
        // Check if a user hub with the same email already exists
        RUserHub existingUserHub = ruhDAO.findByEmail(userHub.getEmail());
        if (existingUserHub != null) {
            throw new IllegalArgumentException("User hub with email " + userHub.getEmail() + " already exists.");
        }
        return ruhDAO.save(userHub);
    }

    public RUser registerRUser(NewRUserDTO RUserDTO, Restaurant restaurant) {
        RUser existingRUser = ruDAO.findByEmailAndRestaurantId(RUserDTO.getEmail(), restaurant.getId());
        if (existingRUser != null) {
            throw new IllegalArgumentException("User already exists for this restaurant.");
        }


        System.out.println("Registering restaurant user with information:" + RUserDTO.getRestaurantId() + " ");
        RestaurantRole role = rrDAO.findById(RUserDTO.getRoleId()).get();

        // Check if a user with the same email already exists
        RUserHub existingUserHub = ruhDAO.findByEmail(RUserDTO.getEmail());
        if (existingUserHub == null) {
            existingUserHub = RUserHub.builder()
                .email(RUserDTO.getEmail())
                .firstName(RUserDTO.getFirstName())
                .lastName(RUserDTO.getLastName())
                .password(RUserDTO.getPassword())
                .build();
            ruhDAO.save(existingUserHub);
        }

        RUser ru = RUser.builder()
            .RUserHub(existingUserHub)
            .email(existingUserHub.getEmail())
            .name(existingUserHub.getFirstName())
            .surname(existingUserHub.getLastName())
            .password(existingUserHub.getPassword())
            .restaurant(restaurant)
            .build();
        Hibernate.initialize(ru.getRoles());
        ru.addRestaurantRole(role);
        existingUserHub.setPassword(passwordEncoder.encode(RUserDTO.getPassword()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    // Add restaurant user bisogna verificare che il ristorante esista

    public RUserDTO addRUserRole(Long idRUser, String string) {
        if ("ROLE_OWNER".equals(string)) {
            throw new IllegalArgumentException("Cannot add the ROLE_OWNER role to a user.");
        }

        RUser ru = ruDAO.findById(idRUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRUser));
        if (ru.getRoles().stream().anyMatch(role -> role.getName().equals(string))) {
            throw new IllegalArgumentException("User already has the role: " + string);
        }
        RestaurantRole role = rrDAO.findByName(string);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + string);
        }
        Hibernate.initialize(ru.getRoles());
        ru.addRestaurantRole(role);
        ruDAO.save(ru);

        return new RUserDTO(ru);
    }

    public void disableRUser(Long idRUser, Long idRUserToDisable) {
        RUser RUserToDisable = ruDAO.findById(idRUserToDisable)
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid restaurant user ID: " + idRUserToDisable));
        if (RUserToDisable.hasRestaurantRole("ROLE_OWNER")) {
            throw new IllegalArgumentException("Cannot disable the owner of the restaurant");
        }
        RUser ru = ruDAO.findById(idRUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRUser));

        String roleNameWithoutPrefix = RUserToDisable.getRoles().stream()
                .map(role -> role.getName().replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User does not have any roles"));
        boolean hasPermission = ru.getRoles().stream()
                .flatMap(role -> role.getPrivileges().stream())
                .anyMatch(privilege -> privilege.getName().equals("PRIVILEGE_DISABLE_" + roleNameWithoutPrefix));

        if (!hasPermission) {
            throw new IllegalArgumentException("User does not have permission to disable this role");
        }

        RUserToDisable.setStatus(RUser.Status.DISABLED);
        ruDAO.save(RUserToDisable);
    }

    public RUserDTO changeRUserRole(Long idRUser, Long idUser, String string) {
        // TODO implementare una logica che capisca se il primo user ha ruolo per
        // cambiare il ruolo del secondo
        RUser ru = ruDAO.findById(idRUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRUser));
        RestaurantRole role = rrDAO.findByName(string);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + string);
        }
        if (ru.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_OWNER"))) {
            throw new IllegalArgumentException("Cannot change role for a user with ROLE_OWNER");
        }
        ru.getRoles().clear();
        Hibernate.initialize(ru.getRoles());
        ru.addRestaurantRole(role);
        // non bisogna creare un nuovo ruolo bisogna mettere ruolo solo
        ruDAO.save(ru);
        return new RUserDTO(ru);
    }

    public boolean checkIfValidOldPassword(final Long id, final String oldPassword) {
        return passwordEncoder.matches(oldPassword, ruDAO.findById(id).get().getPassword());
    }

    public void changeRUserPassword(final Long id, final String password) {
        final RUser ru = ruDAO.findById(id).get();
        ru.getRUserHub().setPassword(passwordEncoder.encode(password));
        ruDAO.save(ru);
    }

    public void createPasswordResetTokenForRUser(final RUser ru, final String token) {
        final RUserPasswordResetToken myToken = new RUserPasswordResetToken(token, ru);
        passwordTokenRepository.save(myToken);
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

       public String validateVerificationToken(String token) {
        final RUserVerificationToken verificationToken = tokenDAO.findByToken(token);
        if (verificationToken == null) {
            return TOKEN_INVALID;
        }

        final RUser user = verificationToken.getRUser();
        final LocalDateTime now = LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            tokenDAO.delete(verificationToken);
            return TOKEN_EXPIRED;
        }
        if (user.getStatus() != RUser.Status.VERIFY_TOKEN) {
            return TOKEN_INVALID;
        }
        user.setStatus(RUser.Status.ENABLED);
        tokenDAO.delete(verificationToken);
        ruDAO.save(user);
        return TOKEN_VALID;
    }

    public RUser getRUser(final String verificationToken) {
        final RUserVerificationToken token = tokenDAO.findByToken(verificationToken);
        if (token != null) {
            return token.getRUser();
        }
        return null;
    }

    public RUserVerificationToken generateNewVerificationToken(final String existingVerificationToken) {
        RUserVerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
        vToken.updateToken(UUID.randomUUID().toString());
        vToken = tokenDAO.save(vToken);
        return vToken;
    }

    public RUser findRUserByEmail(String userEmail) {
        return ruDAO.findByEmail(userEmail);
    }

    public void updateRUserStatus(Long RUserId, RUser.Status newStatus) {
        RUser ru = ruDAO.findById(RUserId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Aggiorna lo stato del customer
        ru.setStatus(newStatus);
        ruDAO.save(ru);

        //TODO:NOn credo ci sia bisogno di rifare jwt in ogni caso fare un test sullo status dopo averlo cambiato
    }

    @Transactional
    public RUserDTO addRUserToRestaurant(NewRUserDTO RUserDTO, Long restaurantId) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID: " + restaurantId));
        RUser RUser = registerRUser(RUserDTO, restaurant);

        // Generate and send verification token
        String token = UUID.randomUUID().toString();
        createVerificationTokenForRUser(RUser, token);
        // TODO: Verificare invio mail
        // final SimpleMailMessage email = constructEmailMessage(event, RUser,
        // token);
        // mailSender.send(email);

        return new RUserDTO(RUser);
    }

    public void createVerificationTokenForRUser(final RUser user, final String token) {
        final RUserVerificationToken myToken = new RUserVerificationToken(token, user);
        tokenDAO.save(myToken);
    }

    public RUserDTO removeRUserRole(Long RUserId, String string) {
        if ("ROLE_OWNER".equals(string)) {
            throw new IllegalArgumentException("Cannot remove the ROLE_OWNER role from a user.");
        }

        RUser ru = ruDAO.findById(RUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + RUserId));

        RestaurantRole role = rrDAO.findByName(string);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + string);
        }

        if (ru.getRoles().stream().noneMatch(r -> r.getName().equals(string))) {
            throw new IllegalArgumentException("User does not have the role: " + string);
        }

        Hibernate.initialize(ru.getRoles());
        ru.removeRole(role);
        ruDAO.save(ru);

        return new RUserDTO(ru);
    }

    public List<RUserDTO> getRUsersByRestaurantId(Long restaurantId) {
        return ruDAO.findByRestaurantId(restaurantId).stream()
            .map(RUserDTO::new)
            .toList();
    }

}