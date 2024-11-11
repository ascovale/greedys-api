package com.application.controller.Socket;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.application.web.socket.Message;
import com.application.web.socket.OutputMessage;

@Controller
public class SocketController {

    public static final String SECURED_CHAT_HISTORY = "/secured/history";
    public static final String SECURED_CHAT = "/secured/chat";
    public static final String SECURED_CHAT_ROOM = "/secured/room";
    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    
    @MessageMapping(SECURED_CHAT)
    @SendTo(SECURED_CHAT_HISTORY)
    public OutputMessage sendAll(Message msg) throws Exception {
        OutputMessage out = new OutputMessage(msg.getFrom(), msg.getText(), new SimpleDateFormat("HH:mm").format(LocalDate.now()));
        return out;
    }

    /**
     * Example of sending message to specific user using 'convertAndSendToUser()' and '/queue'
     */
    @MessageMapping(SECURED_CHAT_ROOM)
    public void sendSpecific(@Payload Message msg, Principal user, @Header("simpSessionId") String sessionId) throws Exception {
        OutputMessage out = new OutputMessage(msg.getFrom(), msg.getText(), new SimpleDateFormat("HH:mm").format(LocalDate.now()));
        simpMessagingTemplate.convertAndSendToUser(msg.getTo(), SECURED_CHAT_SPECIFIC_USER, out);
    }
}