package com.application.admin.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.persistence.dao.AdminDAO;
import com.application.admin.persistence.dao.AdminPasswordResetTokenDAO;
import com.application.admin.persistence.dao.AdminVerificationTokenDAO;
import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.AdminPasswordResetToken;
import com.application.admin.persistence.model.AdminVerificationToken;
import com.application.common.persistence.model.Image;
import com.application.common.web.dto.get.AdminDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminService {

	private final AdminDAO adminDAO;
	private final AdminVerificationTokenDAO tokenDAO;
	private final AdminPasswordResetTokenDAO passwordTokenRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminDTO findById(long id) {
		Admin user = adminDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return new AdminDTO(user);
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
		return adminDAO.getReferenceById(userId);
	}

	public void deleteUserById(Long id) {
		Admin user = adminDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
		deleteUser(user);
	}


	@Transactional
	public void updateAdminStatus(Long adminId, Admin.Status newStatus) {
		log.info("Updating admin status for ID: {} to {}", adminId, newStatus);

		Admin admin = adminDAO.findById(adminId)
				.orElseThrow(() -> new IllegalArgumentException("Admin not found"));
		// Aggiorna lo stato del admin
		log.info("Found admin with ID: {}, updating status to {}", adminId, newStatus);

		admin.setStatus(newStatus);
		adminDAO.save(admin);
		log.info("Admin status updated successfully for ID: {} to {}", adminId, newStatus);
	}

	public AdminDTO loginAndGetDTO(String username) {
		Admin user = findAdminByEmail(username);
		return new AdminDTO(user);
	}

}
