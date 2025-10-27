package com.application.customer.service.authentication;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.CustomerMapper;
import com.application.common.security.jwt.JwtUtil;
import com.application.common.security.jwt.constants.TokenValidationConstants;
import com.application.common.service.EmailService;
import com.application.common.service.authentication.GoogleAuthService;
import com.application.common.web.dto.customer.CustomerDTO;
import com.application.common.web.dto.security.AuthRequestDTO;
import com.application.common.web.dto.security.AuthRequestGoogleDTO;
import com.application.common.web.dto.security.AuthResponseDTO;
import com.application.common.web.error.UserAlreadyExistException;
import com.application.customer.CustomerAuthenticationManager;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.PasswordResetTokenDAO;
import com.application.customer.persistence.dao.RoleDAO;
import com.application.customer.persistence.dao.VerificationTokenDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.Customer.Status;
import com.application.customer.persistence.model.PasswordResetToken;
import com.application.customer.persistence.model.Role;
import com.application.customer.persistence.model.VerificationToken;
import com.application.customer.web.dto.customer.NewCustomerDTO;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CustomerAuthenticationService {

	private final GoogleAuthService googleAuthService;
	private final CustomerDAO customerDAO;
	private final VerificationTokenDAO tokenDAO;
	private final PasswordResetTokenDAO passwordTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RoleDAO roleRepository;
	private final CustomerAuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;
	private final EmailService emailService;
	private final MessageSource messages;
	private final CustomerMapper customerMapper;

	public AuthResponseDTO login(AuthRequestDTO authenticationRequest) {
		log.debug("Authentication request received for username: {}", authenticationRequest.getUsername());

		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
						authenticationRequest.getPassword()));

		log.debug("Authentication successful for username: {}", authenticationRequest.getUsername());

		final Customer customerDetails = customerDAO.findByEmail(authenticationRequest.getUsername());
		if (customerDetails == null) {
			log.warn("No customer found with email: {}", authenticationRequest.getUsername());
			throw new EntityNotFoundException("Customer not found");
		}

		final String jwt = jwtUtil.generateToken(customerDetails);
		log.debug("JWT generated for username: {}", authenticationRequest.getUsername());
		
		// Generiamo refresh token solo se rememberMe è true
		final AuthResponseDTO responseDTO;
		if (authenticationRequest.isRememberMe()) {
			final String refreshToken = jwtUtil.generateRefreshToken(customerDetails);
			responseDTO = AuthResponseDTO.builder()
					.jwt(jwt)
					.refreshToken(refreshToken)
					.user(new CustomerDTO(customerDetails))
					.build();
			log.debug("Refresh token generated for username: {}", authenticationRequest.getUsername());
		} else {
			responseDTO = new AuthResponseDTO(jwt, new CustomerDTO(customerDetails));
		}
		
		return responseDTO;
	}

	public AuthResponseDTO refreshToken(String refreshToken) {
		log.debug("Refresh token request received");
		
		try {
			// Verifica che sia un refresh token valido
			if (!jwtUtil.isRefreshToken(refreshToken)) {
				throw new SecurityException("Invalid refresh token type");
			}
			
			// Estrae l'username dal refresh token
			String username = jwtUtil.extractUsername(refreshToken);
			
			// Trova il customer
			final Customer customerDetails = customerDAO.findByEmail(username);
			if (customerDetails == null) {
				log.warn("No customer found with email from refresh token: {}", username);
				throw new EntityNotFoundException("Customer not found");
			}
			
			// Verifica il refresh token
			if (!jwtUtil.validateToken(refreshToken, customerDetails)) {
				throw new SecurityException("Invalid or expired refresh token");
			}
			
			// Genera nuovi token
			final String newJwt = jwtUtil.generateToken(customerDetails);
			final String newRefreshToken = jwtUtil.generateRefreshToken(customerDetails);
			
			log.debug("New tokens generated for username: {}", username);
			
			return AuthResponseDTO.builder()
					.jwt(newJwt)
					.refreshToken(newRefreshToken)
					.user(new CustomerDTO(customerDetails))
					.build();
					
		} catch (Exception e) {
			log.error("Refresh token validation failed: {}", e.getMessage());
			throw new SecurityException("Invalid refresh token");
		}
	}

	public AuthResponseDTO loginWithGoogle(AuthRequestGoogleDTO authenticationRequest) {
		try {
			return googleAuthService.authenticateWithGoogle(authenticationRequest,
					customerDAO::findByEmail,
					(email, token) -> {
						String[] name = ((String) token.getPayload().get("name")).split(" ");
						NewCustomerDTO accountDto = NewCustomerDTO.builder()
								.email(email)
								.firstName(name[0])
								.lastName(name[1])
								.password(UUID.randomUUID().toString())
								.build();
						return registerNewCustomerAccount(accountDto);
					},
					customer -> {
						// Build response with proper DTO conversion
						String jwt = jwtUtil.generateToken(customer);
						CustomerDTO customerDTO = customerMapper.toDTO(customer);
						return new AuthResponseDTO(jwt, customerDTO);
					});
		} catch (Exception e) {
			log.error("Google authentication failed: {}", e.getMessage(), e);
			throw new RuntimeException("Google authentication failed: " + e.getMessage());
		}
	}

	public CustomerDTO registerNewCustomer(final NewCustomerDTO accountDto)	{
		Customer customer = registerNewCustomerAccount(accountDto);
		return customerMapper.toDTO(customer);
	}
	public Customer registerNewCustomerAccount(final NewCustomerDTO accountDto) {
		if (emailExists(accountDto.getEmail())) {
			throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
		}
		if (accountDto.getPassword() == null) {
			throw new IllegalArgumentException("rawPassword cannot be null");
		}
		final Customer customer = Customer.builder()
				.name(accountDto.getFirstName())
				.surname(accountDto.getLastName())
				.password(passwordEncoder.encode(accountDto.getPassword()))
				.email(accountDto.getEmail())
				.status(Customer.Status.VERIFY_TOKEN) // New customers start as needing email verification
				.roles(new ArrayList<Role>() {
					{
						add(roleRepository.findByName("ROLE_CUSTOMER"));
					}
				})
				.build();
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
		try {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PasswordResetToken getPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token);
	}

	public Customer getCustomerByPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token).getCustomer();
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
			return TokenValidationConstants.TOKEN_INVALID;
		}

		final Customer customer = verificationToken.getCustomer();
		final LocalDateTime now = LocalDateTime.now();
		if (verificationToken.getExpiryDate().isBefore(now)) {
			tokenDAO.delete(verificationToken);
			return TokenValidationConstants.TOKEN_EXPIRED;
		}
		if (customer.getStatus() != Customer.Status.VERIFY_TOKEN) {
			return TokenValidationConstants.TOKEN_INVALID;
		}
		customer.setStatus(Customer.Status.ENABLED);
		tokenDAO.delete(verificationToken);
		customerDAO.save(customer);
		return TokenValidationConstants.TOKEN_VALID;
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
	 * CustomernamePasswordAuthenticationToken(currentCustomer,
	 * currentCustomer.getPassword(),
	 * curAuth.getAuthorities()); SecurityContextHolder.getContext()
	 * .setAuthentication(auth); return currentCustomer; }
	 */

	public void deleteCustomerById(Long id) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		deleteCustomer(customer);
	}

	@Transactional
	public void enableCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.ENABLED);
		customerDAO.save(customer);
	}

	@Transactional
	public void enableCustomerByEmail(String email) {
		Customer customer = customerDAO.findByEmail(email);
		if (customer == null) {
			throw new EntityNotFoundException("Customer not found with email: " + email);
		}
		customer.setStatus(Customer.Status.ENABLED);
		customerDAO.save(customer);
		log.info("Customer enabled by email: {}", email);
	}

	public AuthResponseDTO adminLoginToCustomer(Long customerId, HttpServletRequest request) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("No customer found with ID: " + customerId));

		// Create an authentication token bypassing the password
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
				customer.getEmail(), null);
		authToken.setDetails(customer);

		// Authenticate without password
		SecurityContextHolder.getContext().setAuthentication(authToken);

		final String jwt = jwtUtil.generateToken(customer);
		return new AuthResponseDTO(jwt, new CustomerDTO(customer));
	}

	public void sendPasswordResetEmail(String email, String appUrl, Locale locale) {
		Customer customer = customerDAO.findByEmail(email);
		if (customer == null) {
			throw new RuntimeException("Customer not found");
		}

		String token = UUID.randomUUID().toString();
		createPasswordResetTokenForCustomer(customer, token);
		emailService.sendEmail(constructResetTokenEmail(appUrl, locale, token, customer));
	}

	public void resendRegistrationToken(String existingToken, String appUrl, Locale locale) {
		final VerificationToken newToken = generateNewVerificationToken(existingToken);
		Customer customer = getCustomer(newToken.getToken());
		emailService.sendEmail(constructResendVerificationTokenEmail(appUrl, locale, newToken, customer));
	}

	public AuthResponseDTO authenticateWithGoogle(String token) {
		try {
			GoogleIdToken idToken = verifyGoogleToken(token);

			if (idToken != null) {
				String email = idToken.getPayload().getEmail();
				String name = (String) idToken.getPayload().get("name");

				Customer customer = customerDAO.findByEmail(email);
				if (customer == null) {
					String[] nameParts = name != null ? name.split(" ", 2) : new String[] { "", "" };
					NewCustomerDTO accountDto = NewCustomerDTO.builder()
							.firstName(nameParts[0])
							.lastName(nameParts.length > 1 ? nameParts[1] : "")
							.email(email)
							.password(generateRandomPassword())
							.build();
					customer = registerNewCustomerAccount(accountDto);
				}

				String jwt = jwtUtil.generateToken(customer);
				return new AuthResponseDTO(jwt, new CustomerDTO(customer));
			} else {
				throw new RuntimeException("Google token verification failed");
			}
		} catch (Exception e) {
			throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
		}
	}

	private GoogleIdToken verifyGoogleToken(String token) throws Exception {
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),
					GsonFactory.getDefaultInstance())
					.setAudience(Arrays.asList(
							"982346813437-3s1uepb5ic7ib5r4mfegdsbrkjjvtl7b.apps.googleusercontent.com",
							"982346813437-d0kerhe6h2km0veqs563avsgtv6vb7p5.apps.googleusercontent.com",
							"982346813437-e1vsuujvorosiaamfdc3honrrbur17ri.apps.googleusercontent.com",
							"982346813437-iosclientid.apps.googleusercontent.com"))
					.build();

			return verifier.verify(token);
		} catch (GeneralSecurityException | IOException e) {
			throw new Exception("Google token verification failed", e);
		}
	}

	private SimpleMailMessage constructResendVerificationTokenEmail(final String contextPath, final Locale locale,
			final VerificationToken newToken, final Customer customer) {
		final String confirmationUrl = contextPath + "/registrationConfirm.html?token=" + newToken.getToken();
		final String message = messages.getMessage("message.resendToken", null, locale);
		return constructEmail("Resend Registration Token", message + " \r\n" + confirmationUrl, customer);
	}

	private SimpleMailMessage constructResetTokenEmail(final String contextPath, final Locale locale,
			final String token, final Customer customer) {
		final String url = contextPath + "/customer/changePassword?id=" + customer.getId() + "&token=" + token;
		final String message = messages.getMessage("message.resetPassword", null, locale);
		return constructEmail("Reset Password", message + " \r\n" + url, customer);
	}

	private SimpleMailMessage constructEmail(String subject, String body, Customer customer) {
		final SimpleMailMessage email = new SimpleMailMessage();
		email.setSubject(subject);
		email.setText(body);
		email.setTo(customer.getEmail());
		email.setFrom("reservation@greedys.it");
		return email;
	}

	private String generateRandomPassword() {
		return UUID.randomUUID().toString();
	}

	private boolean emailExists(final String email) {
		return customerDAO.findByEmail(email) != null;
	}

	public void setCustomerStatus(Long id, Status enabled) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setCustomerStatus'");
	}

}
