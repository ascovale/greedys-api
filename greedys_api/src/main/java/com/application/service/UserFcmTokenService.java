package com.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.user.UserFcmTokenDAO;
import com.application.persistence.model.user.User;
import com.application.persistence.model.user.UserFcmToken;
import com.application.web.dto.post.UserFcmTokenDTO;

import jakarta.persistence.EntityManager;

@Service
public class UserFcmTokenService {
    private final UserFcmTokenDAO userFcmTokenRepository;
    private final EntityManager entityManager;

    
    public UserFcmTokenService(UserFcmTokenDAO userFcmTokenRepository, EntityManager entityManager) {
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.entityManager = entityManager;
    }

    public void saveUserFcmToken(UserFcmTokenDTO userFcmTokenDTO) {
        UserFcmToken userFcmToken = new UserFcmToken();
        userFcmToken.setUser(entityManager.getReference(User.class, userFcmTokenDTO.getUserId()));
        userFcmToken.setFcmToken(userFcmTokenDTO.getFcmToken());
    }

    public UserFcmToken updateUserFcmToken(String oldToken, String newToken) {
        UserFcmToken existingToken = userFcmTokenRepository.findByFcmToken(oldToken);
        if (existingToken != null) {
            existingToken.setFcmToken(newToken);
            return userFcmTokenRepository.save(existingToken);
        } else {
            throw new RuntimeException("UserFcmToken not found for user");
        }
    }

    public List<UserFcmToken> getTokensByUserId(Long id) {
        return userFcmTokenRepository.findByUserId(id);
    }
}