package com.application.customer.service.authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.constants.TokenValidationConstants;
import com.application.common.jwt.JwtUtil;
import com.application.common.service.authentication.GoogleAuthService;
import com.application.common.web.dto.AuthRequestGoogleDTO;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.post.AuthRequestDTO;
import com.application.common.web.dto.post.AuthResponseDTO;
import com.application.common.web.error.UserAlreadyExistException;
import com.application.customer.dao.CustomerDAO;
import com.application.customer.dao.PasswordResetTokenDAO;
import com.application.customer.dao.RoleDAO;
import com.application.customer.dao.VerificationTokenDAO;
import com.application.customer.model.Customer;
import com.application.customer.model.PasswordResetToken;
import com.application.customer.model.Role;
import com.application.customer.model.VerificationToken;
import com.application.customer.web.post.NewCustomerDTO;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class CustomerAuthenticationService {
	//TODO perchè non c'è l'admin AuthenticationService?

	public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
	public static String APP_NAME = "SpringRegistration";

	private final GoogleAuthService googleAuthService;
	
	private final CustomerDAO customerDAO;
	private final VerificationTokenDAO tokenDAO;
	private final PasswordResetTokenDAO passwordTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RoleDAO roleRepository;
	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil; 

	public CustomerAuthenticationService(CustomerDAO customerDAO, VerificationTokenDAO tokenDAO,
			PasswordResetTokenDAO passwordTokenRepository,
			PasswordEncoder passwordEncoder,
			RoleDAO roleRepository,
			@Qualifier("customerAuthenticationManager") AuthenticationManager authenticationManager,
			JwtUtil jwtUtil,
			GoogleAuthService googleAuthService) {
		this.customerDAO = customerDAO;
		this.tokenDAO = tokenDAO;
		this.passwordTokenRepository = passwordTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.roleRepository = roleRepository;
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.googleAuthService = googleAuthService;
	}

	public ResponseEntity<?> login(AuthRequestDTO authenticationRequest) {
        
        log.debug("Authentication request received for username: {}", authenticationRequest.getUsername());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()));

            log.debug("Authentication successful for username: {}", authenticationRequest.getUsername());

            final Customer customerDetails = customerDAO.findByEmail(authenticationRequest.getUsername());
            if (customerDetails == null) {
                log.warn("No customer found with email: {}", authenticationRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            final String jwt = jwtUtil.generateToken(customerDetails);
            log.debug("JWT generated for username: {}", authenticationRequest.getUsername());

            final AuthResponseDTO responseDTO = new AuthResponseDTO(jwt, new CustomerDTO(customerDetails));
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            log.error("Authentication failed for username: {}. Error: {}", authenticationRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
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
					jwtUtil::generateToken
					);
		} catch (Exception e) {
			log.error("Google authentication failed: {}", e.getMessage(), e);
			throw new RuntimeException("Google authentication failed: " + e.getMessage());
		}
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
			.roles(new ArrayList<Role>() {{
				add(roleRepository.findByName("ROLE_CUSTOMER"));
			}})
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



	private boolean emailExists(final String email) {
		return customerDAO.findByEmail(email) != null;
	}

}
