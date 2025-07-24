package com.application.customer;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.application.customer.persistence.model.Customer;
import com.application.customer.service.authentication.CustomerAuthenticationService;

import lombok.RequiredArgsConstructor;

@Component  // Temporarily disabled - mail sender dependency conflict
@RequiredArgsConstructor
public class CustomerRegistrationListener implements ApplicationListener<CustomerOnRegistrationCompleteEvent> {
	private final CustomerAuthenticationService service;

	private final MessageSource messages;

	@Qualifier("reservationMailSender")
	private final JavaMailSender mailSender;

	@Override
	public void onApplicationEvent(final CustomerOnRegistrationCompleteEvent event) {
		try {
			this.confirmRegistration(event);
		} catch (MailException e) {
			System.err.println("Failed to send email: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void confirmRegistration(final CustomerOnRegistrationCompleteEvent event) {
		final Customer customer = event.getUser();
		final String token = UUID.randomUUID().toString();
		service.createVerificationTokenForCustomer(customer, token);
		final SimpleMailMessage email = constructEmailMessage(event, customer, token);
		mailSender.send(email);
	}

	private final SimpleMailMessage constructEmailMessage(final CustomerOnRegistrationCompleteEvent event, final Customer customer,
			final String token) {
		System.out.println("Prova inviare");
		final String recipientAddress = customer.getEmail();
		final String subject = "Registration Confirmation";
		final String confirmationUrl = event.getAppUrl() + "/register/customer/confirm?token=" + token;
		final String message = messages.getMessage("message.regSucc", null, event.getLocale());
		final SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(recipientAddress);
		email.setSubject(subject);
		email.setText(message + " \r\n" + confirmationUrl);
		email.setFrom("reservation@greedys.it");
		return email;
	}

}
