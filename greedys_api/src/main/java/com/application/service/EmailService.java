package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.service.rabbitmq.ReservationEmailSenderService;
import com.application.service.rabbitmq.UserEmailSenderService;
import com.application.service.rabbitmq.email.ReservationEmail;
import com.application.service.rabbitmq.email.UserEmail;

@Service
public class EmailService {
	@Autowired
	private UserEmailSenderService userEmailSenderService;
	
	@Autowired
	private ReservationEmailSenderService  reservationEmailSenderService;

	public EmailService(UserEmailSenderService userEmailSenderService,ReservationEmailSenderService reservationEmailSenderService) {
		this.userEmailSenderService = userEmailSenderService;
		this.reservationEmailSenderService = reservationEmailSenderService;
	}

	public void sendUserEmail(UserEmail userEmail) {
		userEmailSenderService.sendEmail(userEmail);
	}

	public void sendReservationEmail(ReservationEmail reservationEmail) {
		reservationEmailSenderService.sendEmail(reservationEmail);
	}
}