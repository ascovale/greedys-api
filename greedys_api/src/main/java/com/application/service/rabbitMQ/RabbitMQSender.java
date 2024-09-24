package com.application.service.rabbitMQ;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.web.socket.NotificationMessage;

//Produttore
@Service
public class RabbitMQSender {

	@Autowired
	private AmqpTemplate amqpTemplate;

	public void send(String message) {
		this.amqpTemplate.convertAndSend("ExchangeName", "ClientRoutingKey", message);
	}
	public void sendNotification(NotificationMessage message) {
        // Invia il messaggio alla coda "RestaurantQueue"
		amqpTemplate.convertAndSend("ExchangeName", "ClientRoutingKey", message);
    }
}
