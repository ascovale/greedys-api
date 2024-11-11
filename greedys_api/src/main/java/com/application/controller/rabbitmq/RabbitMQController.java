package com.application.controller.rabbitmq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.application.service.rabbitmq.RabbitMQSender;


@Controller
public class RabbitMQController {

    @Autowired
    private RabbitMQSender sender;

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(@RequestBody String message) {
        sender.send(message);
        System.out.println("<<<<<<<<<< Sent Message: " + message);
        return ResponseEntity.ok().build();
    }
/* 
    @PostMapping("/send-notifica")
    public ResponseEntity<Void> sendMessage(@RequestBody NotificationMessage message) {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            // Set user information from currentUser to message
            message.setUserId(currentUser.getId());
            message.setEmail(currentUser.getEmail());
            
            // Set other user information if available
            // message.setFirstName(currentUser.getFirstName());
            // message.setLastName(currentUser.getLastName());
            // message.set...
        }
        sender.sendNotification(message);
        System.out.println("<<<<<<<<<< Sent Message: " + message);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }*/
}
