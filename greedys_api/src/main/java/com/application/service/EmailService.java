package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.service.rabbitMQ.ReservationEmailSenderService;
import com.application.service.rabbitMQ.UserEmailSenderService;
import com.application.service.rabbitMQ.email.ReservationEmail;
import com.application.service.rabbitMQ.email.UserEmail;

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