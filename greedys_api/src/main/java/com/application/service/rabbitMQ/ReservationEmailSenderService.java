package com.application.service.rabbitMQ;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.application.service.rabbitMQ.email.ReservationEmail;

//Produttore
@Service
public class ReservationEmailSenderService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void sendEmail(ReservationEmail reservationEmail) {
		rabbitTemplate.convertAndSend("emailExchange", "emailRoutingKey", reservationEmail);
    }
}
