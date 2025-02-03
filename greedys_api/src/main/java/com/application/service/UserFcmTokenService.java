package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.user.UserFcmTokenDAO;
import com.application.persistence.model.user.User;
import com.application.persistence.model.user.UserFcmToken;
import com.application.web.dto.post.UserFcmTokenDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

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
        userFcmToken.setCreatedAt(userFcmTokenDTO.getCreatedAt() != null ? userFcmTokenDTO.getCreatedAt() : LocalDateTime.now());
        userFcmToken.setDeviceId(userFcmTokenDTO.getDeviceId());
        userFcmTokenRepository.save(userFcmToken);
    }

    public UserFcmToken updateUserFcmToken(String oldToken, UserFcmTokenDTO newToken) {
        UserFcmToken existingToken = userFcmTokenRepository.findByFcmTokenAndUserId(oldToken, newToken.getUserId());
        if (existingToken != null) {
            existingToken.setFcmToken(newToken.getFcmToken());
            return userFcmTokenRepository.save(existingToken);
        } else {
            throw new RuntimeException("UserFcmToken not found for user");
        }
    }

    public List<UserFcmToken> getTokensByUserId(Long id) {
        return userFcmTokenRepository.findByUserId(id);
    }

    public UserFcmToken getTokenByDeviceId(String deviceId) {
        return userFcmTokenRepository.findByDeviceId(deviceId);
    }

    public boolean isDeviceTokenPresent(String deviceId) {
        return userFcmTokenRepository.existsByDeviceId(deviceId);
    }

    public String verifyTokenByDeviceId(String deviceId) {
        UserFcmToken userFcmToken = userFcmTokenRepository.findByDeviceId(deviceId);
        if (userFcmToken == null) {
            return "NOT FOUND";
        }
        FirebaseToken decodedToken = verifyToken(userFcmToken.getFcmToken());
        if (decodedToken == null) {
            return "EXPIRED";
        }
        return "OK";
    }

    public FirebaseToken verifyToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (Exception e) {
            // Handle token verification error
            return null;
        }
    }

    
}