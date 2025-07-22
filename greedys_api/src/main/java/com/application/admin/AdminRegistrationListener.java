package com.application.admin;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.application.admin.model.Admin;
import com.application.admin.service.AdminService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminRegistrationListener implements ApplicationListener<AdminOnRegistrationCompleteEvent> {
	private final AdminService service;

	private final MessageSource messages;

	@Qualifier("reservationMailSender")
	private final JavaMailSender mailSender;
	
	// API

	@Override
	public void onApplicationEvent(final AdminOnRegistrationCompleteEvent event) {
	    try {
	        this.confirmRegistration(event);
	    } catch (MailException e) {
	        System.err.println("Failed to send email: " + e.getMessage());
	        e.printStackTrace();
	    }
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
