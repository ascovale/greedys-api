package com.application.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.user.PasswordResetTokenDAO;
import com.application.persistence.dao.user.RoleDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.dao.user.VerificationTokenDAO;
import com.application.persistence.model.Image;
import com.application.persistence.model.user.PasswordResetToken;
import com.application.persistence.model.user.User;
import com.application.persistence.model.user.VerificationToken;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.get.UserDTO;
import com.application.web.dto.post.NewUserDTO;
import com.application.web.error.UserAlreadyExistException;

@Service
@Transactional
public class UserService {

    public static final String TOKEN_INVALID = "invalidToken";
    public static final String TOKEN_EXPIRED = "expired";
    public static final String TOKEN_VALID = "valid";

    public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
    public static String APP_NAME = "SpringRegistration";

    private final UserDAO userDAO;
    private final VerificationTokenDAO tokenDAO;
    private final PasswordResetTokenDAO passwordTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleDAO roleRepository;
    private final SessionRegistry sessionRegistry;
    private final EntityManager entityManager;

    public UserService(UserDAO userDAO, VerificationTokenDAO tokenDAO, 
                       PasswordResetTokenDAO passwordTokenRepository, 
                       @Qualifier("userEncoder") PasswordEncoder passwordEncoder, 
                       RoleDAO roleRepository, 
                       @Qualifier("userSessionRegistry") SessionRegistry sessionRegistry,
                       EntityManager entityManager) {
        this.userDAO = userDAO;
        this.tokenDAO = tokenDAO;
        this.passwordTokenRepository = passwordTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.sessionRegistry = sessionRegistry;
        this.entityManager = entityManager;
    }

	// API

	public UserDTO findById(long id) {
		User user = userDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return new UserDTO(user);
	}

	 
	public User registerNewUserAccount(final NewUserDTO accountDto) {
		if (emailExists(accountDto.getEmail())) {
			throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
		}
		if (accountDto.getPassword() == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
		final User user = new User();

		user.setName(accountDto.getFirstName());
		user.setSurname(accountDto.getLastName());
		user.setPassword(passwordEncoder.encode(accountDto.getPassword()));
		user.setEmail(accountDto.getEmail());
		// user.setUsing2FA(accountDto.isUsing2FA());
		user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
		return userDAO.save(user);
	}

	 
	public User getUser(final String verificationToken) {
		final VerificationToken token = tokenDAO.findByToken(verificationToken);
		if (token != null) {
			return token.getUser();
		}
		return null;
	}

	 
	public VerificationToken getVerificationToken(final String verificationToken) {
		return tokenDAO.findByToken(verificationToken);
	}

	 
	public void saveRegisteredUser(final User user) {
		userDAO.save(user);
	}

	 
	public void deleteUser(final User user) {
		final VerificationToken verificationToken = tokenDAO.findByUser(user);

		if (verificationToken != null) {
			tokenDAO.delete(verificationToken);
		}

		final PasswordResetToken passwordToken = passwordTokenRepository.findByUser(user);

		if (passwordToken != null) {
			passwordTokenRepository.delete(passwordToken);
		}

		userDAO.delete(user);
	}

	public void createVerificationTokenForUser(final User user, final String token) {
		final VerificationToken myToken = new VerificationToken(token, user);
		tokenDAO.save(myToken);
	}

	 
	public VerificationToken generateNewVerificationToken(final String existingVerificationToken) {
		VerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
		vToken.updateToken(UUID.randomUUID().toString());
		vToken = tokenDAO.save(vToken);
		return vToken;
	}

	 
	public void createPasswordResetTokenForUser(final User user, final String token) {
		final PasswordResetToken myToken = new PasswordResetToken(token, user);
		passwordTokenRepository.save(myToken);
	}

	 
	public User findUserByEmail(final String email) {
		return userDAO.findByEmail(email);
	}

	 
	public PasswordResetToken getPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token);
	}

	 
	public User getUserByPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token).getUser();
	}

	 
	public Optional<User> getUserByID(final long id) {
		return userDAO.findById(id);
	}

	 
	public void changeUserPassword(final Long id, final String password) {
		final User user = userDAO.findById(id).get();
		user.setPassword(passwordEncoder.encode(password));
		userDAO.save(user);
	}

	 
	public boolean checkIfValidOldPassword(final Long id, final String oldPassword) {
		return passwordEncoder.matches(oldPassword, userDAO.findById(id).get().getPassword());
	}

	 
	public String validateVerificationToken(String token) {
		final VerificationToken verificationToken = tokenDAO.findByToken(token);
		if (verificationToken == null) {
			return TOKEN_INVALID;
		}

		final User user = verificationToken.getUser();
		final LocalDateTime now = LocalDateTime.now();
		if (verificationToken.getExpiryDate().isBefore(now)) {
			tokenDAO.delete(verificationToken);
			return TOKEN_EXPIRED;
		}

		user.setEnabled(true);
		// tokenDAO.delete(verificationToken);
		userDAO.save(user);
		return TOKEN_VALID;
	}

	/*
	 *   public String generateQRUrl(User user) throws
	 * UnsupportedEncodingException { return QR_PREFIX +
	 * URLEncoder.encode(String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
	 * APP_NAME, user.getEmail(), APP_NAME), "UTF-8"); }
	 * 
	 *   public User updateUser2FA(boolean use2FA) { final Authentication
	 * curAuth = SecurityContextHolder.getContext() .getAuthentication(); User
	 * currentUser = (User) curAuth.getPrincipal(); //
	 * currentUser.setUsing2FA(use2FA); currentUser =
	 * companyuserDAO.save(currentUser); final Authentication auth = new
	 * UsernamePasswordAuthenticationToken(currentUser, currentUser.getPassword(),
	 * curAuth.getAuthorities()); SecurityContextHolder.getContext()
	 * .setAuthentication(auth); return currentUser; }
	 */
	private boolean emailExists(final String email) {
		return userDAO.findByEmail(email) != null;
	}

	 
	public List<String> getUsersFromSessionRegistry() {
		return sessionRegistry.getAllPrincipals().stream()
				.filter((u) -> !sessionRegistry.getAllSessions(u, false).isEmpty()).map(o -> {
					if (o instanceof User) {
						return ((User) o).getEmail();
					} else {
						return o.toString();
					}
				}).collect(Collectors.toList());

	}

	public void save(User user) {
		userDAO.save(user);
	}

	public List<User> findAll() {
		return userDAO.findAll();
	}

	public void addImage(String email,Image image){
		User user = userDAO.findByEmail(email);
		//user.setImage(image);
		//BIsogna aggiungere una lista di immagini
		userDAO.save(user);
	}


    public User getReference(Long userId) {
        return entityManager.getReference(User.class, userId); 
    }


    public Collection<RestaurantDTO> gerRestaurants(Long id) {
		return userDAO.getRestaurants(id).stream().map(r -> new RestaurantDTO(r)).toList();
    }

}
