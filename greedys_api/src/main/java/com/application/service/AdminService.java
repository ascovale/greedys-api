package com.application.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.admin.AdminDAO;
import com.application.persistence.dao.admin.AdminPasswordResetTokenDAO;
import com.application.persistence.dao.admin.AdminRoleDAO;
import com.application.persistence.dao.admin.AdminVerificationTokenDAO;
import com.application.persistence.model.Image;
import com.application.persistence.model.admin.Admin;
import com.application.persistence.model.admin.AdminPasswordResetToken;
import com.application.persistence.model.admin.AdminRole;
import com.application.persistence.model.admin.AdminVerificationToken;
import com.application.web.dto.get.AdminDTO;
import com.application.web.dto.post.admin.NewAdminDTO;
import com.application.web.error.UserAlreadyExistException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class AdminService {

	public static final String TOKEN_INVALID = "invalidToken";
	public static final String TOKEN_EXPIRED = "expired";
	public static final String TOKEN_VALID = "valid";

	public static String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";
	public static String APP_NAME = "SpringRegistration";

	private final AdminDAO adminDAO;
	private final AdminVerificationTokenDAO tokenDAO;
	private final AdminPasswordResetTokenDAO passwordTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminRoleDAO roleRepository;
	private final EntityManager entityManager;
	private final AdminRoleDAO adminRoleDAO;

	public AdminService(AdminDAO adminDAO, AdminVerificationTokenDAO tokenDAO,
	AdminPasswordResetTokenDAO passwordTokenRepository,
			PasswordEncoder passwordEncoder,
			AdminRoleDAO roleRepository,
			EntityManager entityManager,
			AdminRoleDAO adminRoleDAO) {
		this.adminDAO = adminDAO;
		this.tokenDAO = tokenDAO;
		this.passwordTokenRepository = passwordTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.roleRepository = roleRepository;
		this.entityManager = entityManager;
		this.adminRoleDAO = adminRoleDAO;
	}

	public AdminDTO findById(long id) {
		Admin user = adminDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return new AdminDTO(user);
	}

	public Admin registerNewAdminAccount(final NewAdminDTO accountDto) {
		if (emailExists(accountDto.getEmail())) {
			throw new UserAlreadyExistException("There is an account with that email adress: " + accountDto.getEmail());
		}
		if (accountDto.getPassword() == null) {
			throw new IllegalArgumentException("rawPassword cannot be null");
		}
		final Admin admin = new Admin();

		admin.setName(accountDto.getFirstName());
		admin.setSurname(accountDto.getLastName());
		admin.setPassword(passwordEncoder.encode(accountDto.getPassword()));
		admin.setEmail(accountDto.getEmail());
		admin.setStatus(Admin.Status.ENABLED);
		AdminRole adminRole = adminRoleDAO.findByName("ROLE_SUPER_ADMIN");

		admin.setAdminRoles(Arrays.asList(adminRole));
		// user.setUsing2FA(accountDto.isUsing2FA());
		//admin.setAdminRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
		return adminDAO.save(admin);
	}

	public Admin getAdmin(final String verificationToken) {
		AdminVerificationToken token = tokenDAO.findByToken(verificationToken);
		if (token != null) {
			return token.getAdmin();
		}
		return null;
	}

	public AdminVerificationToken getVerificationToken(final String verificationToken) {
		return tokenDAO.findByToken(verificationToken);
	}

	public void saveRegisteredUser(final Admin user) {
		adminDAO.save(user);
	}

	public void deleteUser(final Admin user) {
		final AdminVerificationToken verificationToken = tokenDAO.findByAdmin(user);
		if (verificationToken != null) {
			tokenDAO.delete(verificationToken);
		}
		final AdminPasswordResetToken passwordToken = passwordTokenRepository.findByAdmin(user);
		if (passwordToken != null) {
			passwordTokenRepository.delete(passwordToken);
		}
		adminDAO.delete(user);
	}

	public void createVerificationTokenForAdmin(final Admin admin, final String token) {
		final AdminVerificationToken myToken = new AdminVerificationToken(token, admin);
		tokenDAO.save(myToken);
	}

	public AdminVerificationToken generateNewVerificationToken(final String existingVerificationToken) {
		AdminVerificationToken vToken = tokenDAO.findByToken(existingVerificationToken);
		vToken.updateToken(UUID.randomUUID().toString());
		vToken = tokenDAO.save(vToken);
		return vToken;
	}

	public void createPasswordResetTokenForUser(final Admin user, final String token) {
		final AdminPasswordResetToken myToken = new AdminPasswordResetToken(token, user);
		passwordTokenRepository.save(myToken);
	}

	public Admin findAdminByEmail(final String email) {
		return adminDAO.findByEmail(email);
	}

	public AdminPasswordResetToken getPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token);
	}

	public Admin getAdminByPasswordResetToken(final String token) {
		return passwordTokenRepository.findByToken(token).getAdmin();
	}

	public Optional<Admin> getAdminByID(final long id) {
		return adminDAO.findById(id);
	}

	public void changeAdminPassword(final Long id, final String password) {
		final Admin user = adminDAO.findById(id).get();
		user.setPassword(passwordEncoder.encode(password));
		adminDAO.save(user);
	}

	public boolean checkIfValidOldPassword(final Long id, final String oldPassword) {
		return passwordEncoder.matches(oldPassword, adminDAO.findById(id).get().getPassword());
	}

	public String validateVerificationToken(String token) {
		final AdminVerificationToken verificationToken = tokenDAO.findByToken(token);
		if (verificationToken == null) {
			return TOKEN_INVALID;
		}

		final Admin user = verificationToken.getAdmin();
		final LocalDateTime now = LocalDateTime.now();
		if (verificationToken.getExpiryDate().isBefore(now)) {
			tokenDAO.delete(verificationToken);
			return TOKEN_EXPIRED;
		}

		user.setStatus(Admin.Status.ENABLED);
		//TODO: Cancellare verificationToken anche restaurantUser e Customer
		// tokenDAO.delete(verificationToken);
		adminDAO.save(user);
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
	 * companyadminDAO.save(currentUser); final Authentication auth = new
	 * UsernamePasswordAuthenticationToken(currentUser, currentUser.getPassword(),
	 * curAuth.getAuthorities()); SecurityContextHolder.getContext()
	 * .setAuthentication(auth); return currentUser; }
	 */
	private boolean emailExists(final String email) {
		return adminDAO.findByEmail(email) != null;
	}

	public void save(Admin user) {
		adminDAO.save(user);
	}

	public List<Admin> findAll() {
		return adminDAO.findAll();
	}

	public void addImage(String email, Image image) {
		Admin admin = adminDAO.findByEmail(email);
		// user.setImage(image);
		// BIsogna aggiungere una lista di immagini
		adminDAO.save(admin);
	}

	public Admin getReference(Long userId) {
		return entityManager.getReference(Admin.class, userId);
	}

	public void deleteUserById(Long id) {
		Admin user = adminDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		deleteUser(user);
	}
/* 
	public Admin updateUser(Long id, AdminUserDTO userDto) {
		Admin user = adminDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
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
		return adminDAO.save(user);
	}
*/
/* 
    public void removePermissions(Long idUser) {
		User user = adminDAO.findById(idUser).orElseThrow(() -> new EntityNotFoundException("User not found"));
		user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
		adminDAO.save(user);
	}*/



	public void updateCustomerStatus(Long adminId, Admin.Status newStatus) {
		Admin admin = adminDAO.findById(adminId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found"));

		// Aggiorna lo stato del customer
		admin.setStatus(newStatus);
		adminDAO.save(admin);

		// TODO: gestione della cache
	}

}
