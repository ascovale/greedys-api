package com.application.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
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
import com.application.web.dto.get.AllergyDTO;
import com.application.web.dto.get.CustomerDTO;
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

	private final CustomerDAO customerDAO;
	private final VerificationTokenDAO tokenDAO;
	private final PasswordResetTokenDAO passwordTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RoleDAO roleRepository;
	private final EntityManager entityManager;
	private final AllergyDAO allergyDAO;
	private final PrivilegeDAO privilegeDAO;
	
		public CustomerService(CustomerDAO customerDAO, VerificationTokenDAO tokenDAO,
				PasswordResetTokenDAO passwordTokenRepository,
				PasswordEncoder passwordEncoder,
				RoleDAO roleRepository,
				EntityManager entityManager,
				AllergyDAO allergyDAO,
				PrivilegeDAO privilegeDAO
		) {
			this.customerDAO = customerDAO;
			this.tokenDAO = tokenDAO;
			this.passwordTokenRepository = passwordTokenRepository;
			this.passwordEncoder = passwordEncoder;
			this.roleRepository = roleRepository;
			this.entityManager = entityManager;
			this.privilegeDAO = privilegeDAO;
			this.allergyDAO = allergyDAO;

	}

	public CustomerDTO findById(long id) {
		Customer customer = customerDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		return new CustomerDTO(customer);
	}

	public Customer registerNewCustomerAccount(final NewCustomerDTO accountDto) {
		if (emailExists(accountDto.getEmail())) {
			throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
		}
		if (accountDto.getPassword() == null) {
			throw new IllegalArgumentException("rawPassword cannot be null");
		}
		final Customer customer = new Customer();

		customer.setName(accountDto.getFirstName());
		customer.setSurname(accountDto.getLastName());
		customer.setPassword(passwordEncoder.encode(accountDto.getPassword()));
		customer.setEmail(accountDto.getEmail());
		// customer.setUsing2FA(accountDto.isUsing2FA());
		customer.setRoles(Arrays.asList(roleRepository.findByName("ROLE_CUSTOMER")));
		return customerDAO.save(customer);
	}

	public Customer getCustomer(final String verificationToken) {
		final VerificationToken token = tokenDAO.findByToken(verificationToken);
		if (token != null) {
			return token.getCustomer();
		}
		return null;
	}

	public VerificationToken getVerificationToken(final String verificationToken) {
		return tokenDAO.findByToken(verificationToken);
	}

	public void saveRegisteredCustomer(final Customer customer) {
		customerDAO.save(customer);
	}

	public void deleteCustomer(final Customer customer) {
		final VerificationToken verificationToken = tokenDAO.findByCustomer(customer);
		if (verificationToken != null) {
			tokenDAO.delete(verificationToken);
		}
		final PasswordResetToken passwordToken = passwordTokenRepository.findByCustomer(customer);
		if (passwordToken != null) {
			passwordTokenRepository.delete(passwordToken);
		}
		customerDAO.delete(customer);
	}

	public void createVerificationTokenForCustomer(final Customer customer, final String token) {
		final VerificationToken myToken = new VerificationToken(token, customer);
		tokenDAO.save(myToken);
	}

	public VerificationToken generateNewVerificationToken(final String existingVerificationToken) {
		VerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
		vToken.updateToken(UUID.randomUUID().toString());
		vToken = tokenDAO.save(vToken);
		return vToken;
	}

	public void createPasswordResetTokenForCustomer(final Customer customer, final String token) {
		final PasswordResetToken myToken = new PasswordResetToken(token, customer);
		passwordTokenRepository.save(myToken);
		try{

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Customer findCustomerByEmail(final String email) {
		return customerDAO.findByEmail(email);
	}

	public PasswordResetToken getPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token);
	}

	public Customer getCustomerByPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token).getCustomer();
	}

	public Optional<Customer> getCustomerByID(final long id) {
		return customerDAO.findById(id);
	}

	public void changeCustomerPassword(final Long id, final String password) {
		final Customer customer = customerDAO.findById(id).get();
		customer.setPassword(passwordEncoder.encode(password));
		customerDAO.save(customer);
	}

	public boolean checkIfValidOldPassword(final Long id, final String oldPassword) {
		return passwordEncoder.matches(oldPassword, customerDAO.findById(id).get().getPassword());
	}

	public String validateVerificationToken(String token) {
		final VerificationToken verificationToken = tokenDAO.findByToken(token);
		if (verificationToken == null) {
			return TOKEN_INVALID;
		}

		final Customer customer = verificationToken.getCustomer();
		final LocalDateTime now = LocalDateTime.now();
		if (verificationToken.getExpiryDate().isBefore(now)) {
			tokenDAO.delete(verificationToken);
			return TOKEN_EXPIRED;
		}

		customer.setStatus(Customer.Status.ENABLED);
		// tokenDAO.delete(verificationToken);
		customerDAO.save(customer);
		return TOKEN_VALID;
	}

	/*
	 * public String generateQRUrl(Customer customer) throws
	 * UnsupportedEncodingException { return QR_PREFIX +
	 * URLEncoder.encode(String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
	 * APP_NAME, customer.getEmail(), APP_NAME), "UTF-8"); }
	 * 
	 * public Customer updateCustomer2FA(boolean use2FA) { final Authentication
	 * curAuth = SecurityContextHolder.getContext() .getAuthentication(); Customer
	 * currentCustomer = (Customer) curAuth.getPrincipal(); //
	 * currentCustomer.setUsing2FA(use2FA); currentCustomer =
	 * companycustomerDAO.save(currentCustomer); final Authentication auth = new
	 * CustomernamePasswordAuthenticationToken(currentCustomer, currentCustomer.getPassword(),
	 * curAuth.getAuthorities()); SecurityContextHolder.getContext()
	 * .setAuthentication(auth); return currentCustomer; }
	 */

	private boolean emailExists(final String email) {
		return customerDAO.findByEmail(email) != null;
	}

	public List<Customer> findAll() {
		return customerDAO.findAll();
	}

	public void addImage(String email, Image image) {
		Customer customer = customerDAO.findByEmail(email);
		// customer.setImage(image);
		// BIsogna aggiungere una lista di immagini
		// se si fa add bisogna mettere Hibernate.initialize(customer.getImages());
		customerDAO.save(customer);
	}

	public Customer getReference(Long customerId) {
		return entityManager.getReference(Customer.class, customerId);
	}

	public void deleteCustomerById(Long id) {
		Customer customer = customerDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		deleteCustomer(customer);
	}

	public Customer updateCustomer(Long id, NewCustomerDTO customerDto) {
		Customer customer = customerDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		if (customerDto.getFirstName() != null) {
			customer.setName(customerDto.getFirstName());
		}
		if (customerDto.getLastName() != null) {
			customer.setSurname(customerDto.getLastName());
		}
		if (customerDto.getEmail() != null && !customerDto.getEmail().equals(customer.getEmail())) {
			if (emailExists(customerDto.getEmail())) {
				throw new UserAlreadyExistException(
						"There is an account with that email address: " + customerDto.getEmail());
			}
			customer.setEmail(customerDto.getEmail());
		}
		return customerDAO.save(customer);
	}

	@Transactional
	public void addAllergy(Long idAllergy) {
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		Hibernate.initialize(customer.getAllergies());
		customer.getAllergies().add(allergy);
		customerDAO.save(customer);
	}

	@Transactional
	public void removeAllergy(Long idAllergy) {
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
		.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		customer.getAllergies().remove(allergy);
		customerDAO.save(customer);
	}

	private Customer getCurrentCustomer() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof Customer) {
			return ((Customer) principal);
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}


	@Transactional
	public void enableCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.ENABLED);
		customerDAO.save(customer);
	}
	/*
	 * public void removePermissions(Long idCustomer) {
	 * Customer customer = customerDAO.findById(idCustomer).orElseThrow(() -> new
	 * EntityNotFoundException("Customer not found"));
	 * customer.setRoles(Arrays.asList(roleRepository.findByName("ROLE_CUSTOMER")));
	 * customerDAO.save(customer);
	 * }
	 */

	public void blockCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.DISABLED);
		customerDAO.save(customer);
	}

	public void reportRestaurantAbuse(Long restaurantId) {

		// Assuming there is a ReportAbuseDAO and ReportAbuse entity
		/*
		 * ReportAbuse report = new ReportAbuse();
		 * report.setRestaurantId(restaurantId);
		 * report.setCustomerId(getCurrentCustomer().getId());
		 * report.setTimestamp(LocalDateTime.now());
		 * reportAbuseDAO.save(report);
		 */
		// TODO in futuro decidere cosa farà anche solo mandare una mail
		// L'utente segnala qualche tipo di abuso nella recensione o altro
	}

	@Transactional
	public List<AllergyDTO> getAllergies(Long customerId) {
		Customer customer = customerDAO.findById(customerId).orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		return customer.getAllergies().stream()
				.map(AllergyDTO::new)
				.collect(Collectors.toList());
	}

	public Page<CustomerDTO> findAll(PageRequest pageable) {
		return customerDAO.findAll(pageable).map(CustomerDTO::new);
	}

	public void addRoleToCustomer(Long customerId, String role) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Hibernate.initialize(customer.getRoles());
		customer.getRoles().add(roleRepository.findByName(role));
		customerDAO.save(customer);
	}

	public void removeRoleFromCustomer(Long customerId, String role) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.getRoles().remove(roleRepository.findByName(role));
		customerDAO.save(customer);
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
		Hibernate.initialize(role.getPrivileges());
		role.getPrivileges().add(privilege);
		roleRepository.save(role);
	}

	public void updateCustomerStatus(Long customerId, Customer.Status newStatus) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found"));

		// Aggiorna lo stato del customer
		customer.setStatus(newStatus);
		customerDAO.save(customer);
	}

	public void save(Customer customer) {
		customerDAO.save(customer);
	}

	public void updateFirstName(Long customerId, String firstName) {
		Customer customer = findCustomerById(customerId);
		if (customer == null) {
			throw new IllegalStateException("Customer not found");
		}
		customer.setName(firstName);
		saveCustomer(customer);
	}

	public void updateLastName(Long customerId, String lastName) {
		Customer customer = findCustomerById(customerId);
		if (customer == null) {
			throw new IllegalStateException("Customer not found");
		}
		customer.setSurname(lastName);
		saveCustomer(customer);
	}

	public void updateEmail(Long customerId, String email) {
		Customer customer = findCustomerById(customerId);
		if (customer == null) {
			throw new IllegalStateException("Customer not found");
		}
		customer.setEmail(email);
		saveCustomer(customer);
	}

	private Customer findCustomerById(Long customerId) {
		// Implement logic to retrieve customer by ID from the database
		return null; // Replace with actual implementation
	}

	private void saveCustomer(Customer customer) {
		// Implement logic to save the updated customer to the database
	}

	public void markCustomerHasDeleted(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.AUTO_DELETE);
		customerDAO.save(customer);
	}

	
}
