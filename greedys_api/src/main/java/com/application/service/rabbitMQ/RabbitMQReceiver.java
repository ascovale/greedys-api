package com.application.service.rabbitMQ;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.application.service.MessageService;
import com.application.web.socket.RestaurantNotificationMessage;

@Service
public class RabbitMQReceiver {

    @Autowired
    MessageService messageService;

    @RabbitListener(queues = "RestaurantQueue")
    public void receiveNotification(RestaurantNotificationMessage message) {
        
        // Invia la notifica all'utente tramite WebSocket
        System.out.println("<<<<<Received message: " + "CIAO");
        messageService.sendMessageToUser("ascolesevalentino@gmail.com", "UPDATE");
    }
/* 
    @RabbitListener(queues = "RestaurantQueue")
    public void receive(String message) {
        System.out.println(">>>>>>>> Received Message: " + message);
		simpMessagingTemplate.convertAndSend(SECURED_CHAT_HISTORY, new OutputMessage(message, message, message));
    }*/
 
}