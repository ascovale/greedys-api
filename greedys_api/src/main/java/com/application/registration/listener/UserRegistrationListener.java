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

import com.application.service.UserService;
import com.application.persistence.model.user.User;
import com.application.registration.UserOnRegistrationCompleteEvent;

@Component
public class UserRegistrationListener implements ApplicationListener<UserOnRegistrationCompleteEvent> {
	@Autowired
	private UserService service;

	@Autowired
	private MessageSource messages;

	@Autowired
	@Qualifier("getUserMailSender")
	private JavaMailSender mailSender;

	@Autowired
	private Environment env;

	// API

	@Override
	public void onApplicationEvent(final UserOnRegistrationCompleteEvent event) {
		this.confirmRegistration(event);
	}

	private void confirmRegistration(final UserOnRegistrationCompleteEvent event) {
		final User user = event.getUser();
		final String token = UUID.randomUUID().toString();
		service.createVerificationTokenForUser(user, token);
		final SimpleMailMessage email = constructEmailMessage(event, user, token);
		mailSender.send(email);
	}

	private final SimpleMailMessage constructEmailMessage(final UserOnRegistrationCompleteEvent event, final User user,
			final String token) {
		System.out.println("Prova inviare");
		final String recipientAddress = user.getEmail();
		final String subject = "Registration Confirmation";
		final String confirmationUrl = event.getAppUrl() + "/register/user/confirm?token=" + token;
		final String message = messages.getMessage("message.regSucc", null, event.getLocale());
		final SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(recipientAddress);
		email.setSubject(subject);
		email.setText(message + " \r\n" + confirmationUrl);
		email.setFrom(env.getProperty("support.email"));
		return email;
	}

}
