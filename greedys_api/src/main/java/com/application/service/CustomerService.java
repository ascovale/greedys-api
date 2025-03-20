package com.application.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.AllergyDAO;
import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.PasswordResetTokenDAO;
import com.application.persistence.dao.customer.PrivilegeDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.dao.customer.VerificationTokenDAO;
import com.application.persistence.model.Image;
import com.application.persistence.model.customer.Allergy;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.PasswordResetToken;
import com.application.persistence.model.customer.Privilege;
import com.application.persistence.model.customer.Role;
import com.application.persistence.model.customer.VerificationToken;
import com.application.web.dto.AllergyDTO;
import com.application.web.dto.get.UserDTO;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.error.UserAlreadyExistException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class CustomerService {

	public static final String TOKEN_INVALID = "invalidToken";
	public static final String TOKEN_EXPIRED = "expired";
	public static final String TOKEN_VALID = "valid";

	public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
	public static String APP_NAME = "SpringRegistration";

	private final CustomerDAO userDAO;
	private final VerificationTokenDAO tokenDAO;
	private final PasswordResetTokenDAO passwordTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RoleDAO roleRepository;
	private final EntityManager entityManager;
	private final AllergyDAO allergyDAO;
	private final PrivilegeDAO privilegeDAO;
	
		public CustomerService(CustomerDAO userDAO, VerificationTokenDAO tokenDAO,
				PasswordResetTokenDAO passwordTokenRepository,
				PasswordEncoder passwordEncoder,
				RoleDAO roleRepository,
				EntityManager entityManager,
				AllergyDAO allergyDAO,
				PrivilegeDAO privilegeDAO
				) {
			this.userDAO = userDAO;
			this.tokenDAO = tokenDAO;
			this.passwordTokenRepository = passwordTokenRepository;
			this.passwordEncoder = passwordEncoder;
			this.roleRepository = roleRepository;
			this.entityManager = entityManager;
			this.privilegeDAO = privilegeDAO;
			this.allergyDAO = allergyDAO;

	}

	// API

	public UserDTO findById(long id) {
		Customer user = userDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return new UserDTO(user);
	}

	public Customer registerNewUserAccount(final NewCustomerDTO accountDto) {
		if (emailExists(accountDto.getEmail())) {
			throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
		}
		if (accountDto.getPassword() == null) {
			throw new IllegalArgumentException("rawPassword cannot be null");
		}
		final Customer user = new Customer();

		user.setName(accountDto.getFirstName());
		user.setSurname(accountDto.getLastName());
		user.setPassword(passwordEncoder.encode(accountDto.getPassword()));
		user.setEmail(accountDto.getEmail());
		// user.setUsing2FA(accountDto.isUsing2FA());
		user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
		return userDAO.save(user);
	}

	public Customer getUser(final String verificationToken) {
		final VerificationToken token = tokenDAO.findByToken(verificationToken);
		if (token != null) {
			return token.getCustomer();
		}
		return null;
	}

	public VerificationToken getVerificationToken(final String verificationToken) {
		return tokenDAO.findByToken(verificationToken);
	}

	public void saveRegisteredUser(final Customer user) {
		userDAO.save(user);
	}

	public void deleteUser(final Customer user) {
		final VerificationToken verificationToken = tokenDAO.findByCustomer(user);
		if (verificationToken != null) {
			tokenDAO.delete(verificationToken);
		}
		final PasswordResetToken passwordToken = passwordTokenRepository.findByCustomer(user);
		if (passwordToken != null) {
			passwordTokenRepository.delete(passwordToken);
		}
		userDAO.delete(user);
	}

	public void createVerificationTokenForUser(final Customer user, final String token) {
		final VerificationToken myToken = new VerificationToken(token, user);
		tokenDAO.save(myToken);
	}

	public VerificationToken generateNewVerificationToken(final String existingVerificationToken) {
		VerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
		vToken.updateToken(UUID.randomUUID().toString());
		vToken = tokenDAO.save(vToken);
		return vToken;
	}

	public void createPasswordResetTokenForUser(final Customer user, final String token) {
		final PasswordResetToken myToken = new PasswordResetToken(token, user);
		passwordTokenRepository.save(myToken);
	}

	public Customer findUserByEmail(final String email) {
		return userDAO.findByEmail(email);
	}

	public PasswordResetToken getPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token);
	}

	public Customer getUserByPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token).getCustomer();
	}

	public Optional<Customer> getUserByID(final long id) {
		return userDAO.findById(id);
	}

	public void changeUserPassword(final Long id, final String password) {
		final Customer user = userDAO.findById(id).get();
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

		final Customer user = verificationToken.getCustomer();
		final LocalDateTime now = LocalDateTime.now();
		if (verificationToken.getExpiryDate().isBefore(now)) {
			tokenDAO.delete(verificationToken);
			return TOKEN_EXPIRED;
		}

		user.setStatus(Customer.Status.ENABLED);
		// tokenDAO.delete(verificationToken);
		userDAO.save(user);
		return TOKEN_VALID;
	}

	/*
	 * public String generateQRUrl(User user) throws
	 * UnsupportedEncodingException { return QR_PREFIX +
	 * URLEncoder.encode(String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
	 * APP_NAME, user.getEmail(), APP_NAME), "UTF-8"); }
	 * 
	 * public User updateUser2FA(boolean use2FA) { final Authentication
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

	public List<Customer> findAll() {
		return userDAO.findAll();
	}

	public void addImage(String email, Image image) {
		Customer user = userDAO.findByEmail(email);
		// user.setImage(image);
		// BIsogna aggiungere una lista di immagini
		userDAO.save(user);
	}

	public Customer getReference(Long userId) {
		return entityManager.getReference(Customer.class, userId);
	}

	public void deleteUserById(Long id) {
		Customer user = userDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		deleteUser(user);
	}

	public Customer updateUser(Long id, NewCustomerDTO userDto) {
		Customer user = userDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		if (userDto.getFirstName() != null) {
			user.setName(userDto.getFirstName());
		}
		if (userDto.getLastName() != null) {
			user.setSurname(userDto.getLastName());
		}
		if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
			if (emailExists(userDto.getEmail())) {
				throw new UserAlreadyExistException(
						"There is an account with that email address: " + userDto.getEmail());
			}
			user.setEmail(userDto.getEmail());
		}
		return userDAO.save(user);
	}

	@Transactional
	public void addAllergy(Long idAllergy) {
		Customer user = getCurrentUser();
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		user.getAllergies().add(allergy);
		userDAO.save(user);
	}

	@Transactional
	public void removeAllergy(Long idAllergy) {
		Customer user = getCurrentUser();
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		user.getAllergies().remove(allergy);
		userDAO.save(user);
	}

	private Customer getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof Customer) {
			return ((Customer) principal);
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}

	@Transactional
	public void createAllergy(AllergyDTO allergyDto) {
		Allergy allergy = new Allergy();
		allergy.setName(allergyDto.getName());
		allergy.setDescription(allergyDto.getDescription());
		allergyDAO.save(allergy);
	}

	public void deleteAllergy(Long idAllergy) {
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		allergyDAO.delete(allergy);
	}

	@Transactional
	public void modifyAllergy(Long idAllergy, AllergyDTO allergyDto) {
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		if (allergyDto.getName() != null) {
			allergy.setName(allergyDto.getName());
		}
		if (allergyDto.getDescription() != null) {
			allergy.setDescription(allergyDto.getDescription());
		}
		allergyDAO.save(allergy);
	}

	@Transactional
	public void enableUser(Long userId) {
		Customer user = userDAO.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		user.setStatus(Customer.Status.ENABLED);
		userDAO.save(user);
	}
	/*
	 * public void removePermissions(Long idUser) {
	 * User user = userDAO.findById(idUser).orElseThrow(() -> new
	 * EntityNotFoundException("User not found"));
	 * user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
	 * userDAO.save(user);
	 * }
	 */

	public void blockUser(Long userId) {
		Customer user = userDAO.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		user.setStatus(Customer.Status.DISABLED);
		userDAO.save(user);
	}

	public void reportRestaurantAbuse(Long restaurantId) {

		// Assuming there is a ReportAbuseDAO and ReportAbuse entity
		/*
		 * ReportAbuse report = new ReportAbuse();
		 * report.setRestaurantId(restaurantId);
		 * report.setUserId(getCurrentUser().getId());
		 * report.setTimestamp(LocalDateTime.now());
		 * reportAbuseDAO.save(report);
		 */
		// TODO in futuro decidere cosa farà anche solo mandare una mail
		// L'utente segnala qualche tipo di abuso nella recensione o altro
	}

	@Transactional
	public List<String> getAllergies(Long userId) {
		Customer user = userDAO.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return user.getAllergies().stream().map(Allergy::getName).collect(Collectors.toList());
	}

	public Page<UserDTO> findAll(PageRequest pageable) {
		return userDAO.findAll(pageable).map(UserDTO::new);
	}

	public void addRoleToCustomer(Long customerId, String role) {
		Customer customer = userDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.getRoles().add(roleRepository.findByName(role));
		userDAO.save(customer);
	}

	public void removeRoleFromCustomer(Long customerId, String role) {
		Customer customer = userDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.getRoles().remove(roleRepository.findByName(role));
		userDAO.save(customer);
	}

	public void addPrivilegeToRole(String roleName, String privilegeName) {
		Role role = roleRepository.findByName(roleName);
		if (role == null) {
			throw new EntityNotFoundException("Role not found");
		}
		Privilege privilege = privilegeDAO.findByName(privilegeName);
		if (privilege == null) {
			throw new EntityNotFoundException("Permission not found");
		}
		role.getPrivileges().add(privilege);
		roleRepository.save(role);
	}

	public void updateCustomerStatus(Long customerId, Customer.Status newStatus) {
		Customer customer = userDAO.findById(customerId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found"));

		// Aggiorna lo stato del customer
		customer.setStatus(newStatus);
		userDAO.save(customer);
	}

	public void save(Customer user) {
		userDAO.save(user);
	}

}
