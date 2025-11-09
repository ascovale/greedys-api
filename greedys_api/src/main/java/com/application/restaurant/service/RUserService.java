package com.application.restaurant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.RUserMapper;
import com.application.common.security.jwt.constants.TokenValidationConstants;
import com.application.common.service.EmailService;
import com.application.common.web.dto.restaurant.RUserDTO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RUserHubDAO;
import com.application.restaurant.persistence.dao.RUserHubVerificationTokenDAO;
import com.application.restaurant.persistence.dao.RUserPasswordResetTokenDAO;
import com.application.restaurant.persistence.dao.RUserVerificationTokenDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RestaurantRoleDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.persistence.model.user.RUserHub;
import com.application.restaurant.persistence.model.user.RUserHubVerificationToken;
import com.application.restaurant.persistence.model.user.RUserPasswordResetToken;
import com.application.restaurant.persistence.model.user.RUserVerificationToken;
import com.application.restaurant.persistence.model.user.RestaurantRole;
import com.application.restaurant.web.dto.staff.NewRUserDTO;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RUserService {

    public static final String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
    public static final String APP_NAME = "SpringRegistration";

    private final RUserVerificationTokenDAO tokenDAO;
    private final RUserHubVerificationTokenDAO hubTokenDAO;
    private final EmailService emailService;
    private final RUserDAO ruDAO;
    private final RestaurantDAO restaurantDAO;
    private final RestaurantRoleDAO rrDAO;
    private final RUserHubDAO ruhDAO;
    private final PasswordEncoder passwordEncoder;
    private final RUserPasswordResetTokenDAO passwordTokenRepository;
    private final RUserMapper rUserMapper;

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
        
        // Nuovo Hub inizia sempre in stato VERIFY_TOKEN
        userHub.setStatus(RUserHub.Status.VERIFY_TOKEN);
        RUserHub savedHub = ruhDAO.save(userHub);
        
        // Genera e invia token di verifica per l'Hub
        String token = UUID.randomUUID().toString();
        createVerificationTokenForHub(savedHub, token);
        
        // TODO: Inviare email di verifica
        // emailService.sendHubVerificationEmail(savedHub, token);
        
        return savedHub;
    }

    public RUser registerRUser(NewRUserDTO RUserDTO, Restaurant restaurant) {
        RUser existingRUser = ruDAO.findByEmailAndRestaurantId(RUserDTO.getEmail(), restaurant.getId());
        if (existingRUser != null) {
            throw new IllegalArgumentException("User already exists for this restaurant.");
        }

        System.out.println("Registering restaurant user with information:" + restaurant.getId() + " ");
        RestaurantRole role = rrDAO.findById(RUserDTO.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + RUserDTO.getRoleId()));

        // Check if a user with the same email already exists
        RUserHub existingUserHub = ruhDAO.findByEmail(RUserDTO.getEmail());
        if (existingUserHub == null) {
            // Create new RUserHub - inizia con verifica email richiesta
            existingUserHub = RUserHub.builder()
                .email(RUserDTO.getEmail())
                .firstName(RUserDTO.getFirstName())
                .lastName(RUserDTO.getLastName())
                .password(passwordEncoder.encode(RUserDTO.getPassword()))
                .status(RUserHub.Status.VERIFY_TOKEN) // Nuovo Hub richiede verifica
                .build();
            existingUserHub = ruhDAO.save(existingUserHub);
            
            // Genera token di verifica per il nuovo Hub
            String hubToken = UUID.randomUUID().toString();
            createVerificationTokenForHub(existingUserHub, hubToken);
            // TODO: emailService.sendHubVerificationEmail(existingUserHub, hubToken);
        } else {
            // Hub già esistente - aggiorna solo informazioni base (non password per sicurezza)
            existingUserHub.setFirstName(RUserDTO.getFirstName());
            existingUserHub.setLastName(RUserDTO.getLastName());
            existingUserHub = ruhDAO.save(existingUserHub);
        }

        // Create RUser ereditando lo status dall'Hub
        RUser.Status rUserStatus = mapHubStatusToUserStatus(existingUserHub.getStatus());
        
        RUser ru = RUser.builder()
            .RUserHub(existingUserHub)
            .email(existingUserHub.getEmail())
            .name(existingUserHub.getFirstName())
            .surname(existingUserHub.getLastName())
            .password(existingUserHub.getPassword())
            .restaurant(restaurant)
            .status(rUserStatus) // Eredita status dall'Hub
            .build();
        Hibernate.initialize(ru.getRoles());
        ru.addRestaurantRole(role);
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

        return rUserMapper.toDTO(ru);
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
        return rUserMapper.toDTO(ru);
    }

    public boolean checkIfValidOldPassword(final Long id, final String oldPassword) {
        RUser user = ruDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return passwordEncoder.matches(oldPassword, user.getPassword());
    }

    public void changeRUserPassword(final Long id, final String password) {
        final RUser ru = ruDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
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
            return TokenValidationConstants.TOKEN_INVALID;
        }

        final RUser user = verificationToken.getRUser();
        final LocalDateTime now = LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            tokenDAO.delete(verificationToken);
            return TokenValidationConstants.TOKEN_EXPIRED;
        }
        if (user.getStatus() != RUser.Status.VERIFY_TOKEN) {
            return TokenValidationConstants.TOKEN_INVALID;
        }
        user.setStatus(RUser.Status.ENABLED);
        tokenDAO.delete(verificationToken);
        ruDAO.save(user);
        return TokenValidationConstants.TOKEN_VALID;
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

    public RUserDTO getRUserByEmail(String userEmail) {
        RUser rUser = ruDAO.findByEmail(userEmail);
        if (rUser == null) {
            throw new IllegalArgumentException("Restaurant user not found with email: " + userEmail);
        }
        return rUserMapper.toDTO(rUser);
    }

    public List<RUserDTO> getAllRUsers() {
        List<RUser> rUsers = ruDAO.findAll();
        return rUsers.stream()
                .map(rUserMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<RUserDTO> getAllRUsers(Pageable pageable) {
        return ruDAO.findAll(pageable)
                .map(rUserMapper::toDTO);
    }

    public void updateRUserStatus(Long RUserId, RUser.Status newStatus) {
        RUser ru = ruDAO.findById(RUserId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant user not found"));

        // Aggiorna lo stato del restaurant user
        ru.setStatus(newStatus);
        ruDAO.save(ru);

    }

    public void updateRUserStatusByEmail(String email, RUser.Status newStatus) {
        List<RUser> users = ruDAO.findAllByEmail(email);
        if (users.isEmpty()) {
            throw new IllegalArgumentException("No users found with email: " + email);
        }

        // Aggiorna lo stato di tutti gli utenti con questa email
        for (RUser user : users) {
            user.setStatus(newStatus);
            ruDAO.save(user);
        }
    }

    public RUserDTO addRUserToRestaurant(NewRUserDTO RUserDTO, Long restaurantId) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID: " + restaurantId));
        RUser RUser = registerRUser(RUserDTO, restaurant);

        // Non generiamo più token individuali - la verifica è gestita dall'Hub
        // Se l'Hub è nuovo, il token è già stato generato in registerRUser
        // Se l'Hub esiste già ed è verificato, l'RUser eredita lo status ENABLED

        return rUserMapper.toDTO(RUser);
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

        return rUserMapper.toDTO(ru);
    }

    public List<RUserDTO> getRUsersByRestaurantId(Long restaurantId) {
        return ruDAO.findByRestaurantId(restaurantId).stream()
            .map(rUserMapper::toDTO)
            .toList();
    }

    public Page<RUserDTO> getRUsersByRestaurantId(Long restaurantId, Pageable pageable) {
        return ruDAO.findByRestaurantId(restaurantId, pageable)
            .map(rUserMapper::toDTO);
    }

    /**
     * Aggiunge un ristorante esistente ad un RUserHub esistente
     * 
     * @param hubEmail Email dell'hub utente
     * @param restaurantId ID del ristorante da aggiungere
     * @param roleId ID del ruolo da assegnare all'utente per questo ristorante
     * @return RUserDTO del nuovo utente creato
     * @throws IllegalArgumentException se hub, ristorante o ruolo non esistono, o se l'utente esiste già
     */
    public RUserDTO addRestaurantToHub(String hubEmail, Long restaurantId, Long roleId) {
        // Trova l'hub esistente
        RUserHub hub = ruhDAO.findByEmail(hubEmail);
        if (hub == null) {
            throw new IllegalArgumentException("Hub user not found with email: " + hubEmail);
        }
        
        // Trova il ristorante esistente
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));
        
        // Verifica che non esista già un RUser per questo hub e ristorante
        RUser existingRUser = ruDAO.findByEmailAndRestaurantId(hubEmail, restaurantId);
        if (existingRUser != null) {
            throw new IllegalArgumentException("User already exists for this restaurant.");
        }
        
        // Trova il ruolo
        RestaurantRole role = rrDAO.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        
        // Crea nuovo RUser collegato all'hub e al ristorante
        RUser ru = RUser.builder()
                .RUserHub(hub)
                .email(hub.getEmail())
                .name(hub.getFirstName())
                .surname(hub.getLastName())
                .password(hub.getPassword()) // Usa password dell'hub
                .restaurant(restaurant)
                .status(RUser.Status.ENABLED) // Abilitato di default se l'hub è già verificato
                .build();
        
        Hibernate.initialize(ru.getRoles());
        ru.addRestaurantRole(role);
        ruDAO.save(ru);
        
        return rUserMapper.toDTO(ru);
    }

    /**
     * Retrieves complete restaurant details (RestaurantDTO) for a restaurant ID.
     * This is used to return restaurant information to the authenticated restaurant user.
     *
     * @param restaurantId The ID of the restaurant
     * @return RestaurantDTO with all restaurant details (name, email, address, etc.)
     * @throws IllegalArgumentException if restaurant not found
     */
    public com.application.common.web.dto.restaurant.RestaurantDTO getRestaurantDetails(Long restaurantId) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + restaurantId));
        
        // Map Restaurant entity to RestaurantDTO
        return com.application.common.web.dto.restaurant.RestaurantDTO.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .email(restaurant.getEmail())
                .address(restaurant.getAddress())
                .post_code(restaurant.getPostCode())
                .vatNumber(restaurant.getVatNumber())
                .build();
    }

    // ===== HUB VERIFICATION METHODS =====

    /**
     * Crea un token di verifica per RUserHub
     */
    public void createVerificationTokenForHub(final RUserHub userHub, final String token) {
        final RUserHubVerificationToken hubToken = new RUserHubVerificationToken(token, userHub);
        hubTokenDAO.save(hubToken);
    }

    /**
     * Valida il token di verifica per RUserHub
     */
    public String validateHubVerificationToken(String token) {
        final RUserHubVerificationToken verificationToken = hubTokenDAO.findByToken(token);
        if (verificationToken == null) {
            return TokenValidationConstants.TOKEN_INVALID;
        }

        final RUserHub userHub = verificationToken.getRUserHub();
        final LocalDateTime now = LocalDateTime.now();
        if (verificationToken.getExpiryDate().isBefore(now)) {
            hubTokenDAO.delete(verificationToken);
            return TokenValidationConstants.TOKEN_EXPIRED;
        }
        if (userHub.getStatus() != RUserHub.Status.VERIFY_TOKEN) {
            return TokenValidationConstants.TOKEN_INVALID;
        }
        
        // Aggiorna status Hub a ENABLED
        userHub.setStatus(RUserHub.Status.ENABLED);
        ruhDAO.save(userHub);
        
        // Aggiorna tutti gli RUser associati a questo Hub
        updateAllRUserStatusByHub(userHub);
        
        // Elimina token usato
        hubTokenDAO.delete(verificationToken);
        return TokenValidationConstants.TOKEN_VALID;
    }

    /**
     * Mappa lo status dell'Hub allo status dell'RUser
     */
    private RUser.Status mapHubStatusToUserStatus(RUserHub.Status hubStatus) {
        switch (hubStatus) {
            case VERIFY_TOKEN:
                return RUser.Status.VERIFY_TOKEN;
            case ENABLED:
                return RUser.Status.ENABLED;
            case BLOCKED:
                return RUser.Status.BLOCKED;
            case DELETED:
                return RUser.Status.DELETED;
            case DISABLED:
                return RUser.Status.DISABLED;
            default:
                return RUser.Status.VERIFY_TOKEN;
        }
    }

    /**
     * Aggiorna lo status di tutti gli RUser associati a un Hub
     */
    private void updateAllRUserStatusByHub(RUserHub userHub) {
        List<RUser> associatedUsers = ruDAO.findAllByRUserHubId(userHub.getId());
        RUser.Status newStatus = mapHubStatusToUserStatus(userHub.getStatus());
        
        for (RUser user : associatedUsers) {
            user.setStatus(newStatus);
            ruDAO.save(user);
        }
    }

}
