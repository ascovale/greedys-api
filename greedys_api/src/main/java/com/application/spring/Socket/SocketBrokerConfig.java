package com.application.spring.Socket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.application.persistence.model.user.User;

@Configuration
@EnableWebSocketMessageBroker
public class SocketBrokerConfig implements WebSocketMessageBrokerConfigurer {
	public static final String SECURED_CHAT_HISTORY = "/secured/history";
	public static final String SECURED_CHAT_ROOM = "/secured/room";
	public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/notifications";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(SECURED_CHAT_HISTORY, SECURED_CHAT_SPECIFIC_USER);
        config.setApplicationDestinationPrefixes("/socket");
        config.setUserDestinationPrefix("/secured/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(SECURED_CHAT_ROOM).withSockJS();
    }
	
   
}

