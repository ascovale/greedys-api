package com.application.service.rabbitMQ;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.service.rabbitMQ.email.UserEmail;


//Produttore
@Service
public class UserEmailSenderService {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void sendEmail(UserEmail userEmail) {
		rabbitTemplate.convertAndSend("emailExchange", "emailRoutingKey", userEmail);
    }
}
