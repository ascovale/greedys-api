package com.application.registration.listener;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.application.persistence.model.admin.Admin;
import com.application.registration.AdminOnRegistrationCompleteEvent;
import com.application.service.AdminService;

@Component
public class AdminRegistrationListener implements ApplicationListener<AdminOnRegistrationCompleteEvent> {
	@Autowired
	private AdminService service;

	@Autowired
	private MessageSource messages;

	@Autowired
	@Qualifier("reservationMailSender")
	private JavaMailSender mailSender;
	
	// API

	@Override
	public void onApplicationEvent(final AdminOnRegistrationCompleteEvent event) {
		this.confirmRegistration(event);
	}

	private void confirmRegistration(final AdminOnRegistrationCompleteEvent event) {
		final Admin admin = event.getAdmin();
		final String token = UUID.randomUUID().toString();
		service.createVerificationTokenForAdmin(admin, token);
		final SimpleMailMessage email = constructEmailMessage(event, admin, token);
		mailSender.send(email);
	}

	private final SimpleMailMessage constructEmailMessage(final AdminOnRegistrationCompleteEvent event, final Admin admin,
			final String token) {
		System.out.println("Prova inviare");
		final String recipientAddress = admin.getEmail();
		final String subject = "Registration Confirmation";
		final String confirmationUrl = event.getAppUrl() + "/register/admin/confirm?token=" + token;
		final String message = messages.getMessage("message.regSucc", null, event.getLocale());
		final SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(recipientAddress);
		email.setSubject(subject);
		email.setText(message + " \r\n" + confirmationUrl);
		email.setFrom("reservation@greedys.it");
		return email;
	}

}
