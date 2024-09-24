package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
public class MessageService {
    @Autowired
    private final SimpMessagingTemplate simpMessagingTemplate;


    public MessageService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }
    
    @MessageMapping("/secured/room") 
    public void sendMessageToUser(String email, Object message) {
        simpMessagingTemplate.convertAndSendToUser(email, "/secured/user/queue/notifications", message);
    }
}