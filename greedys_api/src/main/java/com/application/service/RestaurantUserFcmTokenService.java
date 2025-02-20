package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.application.persistence.dao.restaurant.RestaurantUserFcmTokenDAO;
import com.application.persistence.model.restaurant.user.RestaurantUserFcmToken;
import com.application.web.dto.post.UserFcmTokenDTO;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.persistence.EntityManager;

@Service
public class RestaurantUserFcmTokenService {
    private final RestaurantUserFcmTokenDAO userFcmTokenRepository;
    private final EntityManager entityManager;

    public RestaurantUserFcmTokenService(RestaurantUserFcmTokenDAO userFcmTokenRepository, EntityManager entityManager) {
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.entityManager = entityManager;
    }

    public void saveUserFcmToken(UserFcmTokenDTO userFcmTokenDTO) {
        RestaurantUserFcmToken userFcmToken = new RestaurantUserFcmToken();
        userFcmToken.setUser(entityManager.getReference(RestaurantUser.class, userFcmTokenDTO.getUserId()));
        userFcmToken.setFcmToken(userFcmTokenDTO.getFcmToken());
        userFcmToken.setCreatedAt(userFcmTokenDTO.getCreatedAt() != null ? userFcmTokenDTO.getCreatedAt() : LocalDateTime.now());
        userFcmToken.setDeviceId(userFcmTokenDTO.getDeviceId());
        userFcmTokenRepository.save(userFcmToken);
    }

    public RestaurantUserFcmToken updateUserFcmToken(String oldToken, UserFcmTokenDTO newToken) {
        RestaurantUserFcmToken existingToken = userFcmTokenRepository.findByFcmTokenAndRestaurantUserId(oldToken, newToken.getUserId());
        if (existingToken != null) {
            existingToken.setFcmToken(newToken.getFcmToken());
            return userFcmTokenRepository.save(existingToken);
        } else {
            throw new RuntimeException("UserFcmToken not found for user");
        }
    }

    public List<RestaurantUserFcmToken> getTokensByRestaurantUserId(Long id) {
        return userFcmTokenRepository.findByRestaurantUserId(id);
    }

    public RestaurantUserFcmToken getTokenByDeviceId(String deviceId) {
        return userFcmTokenRepository.findByDeviceId(deviceId);
    }

    public boolean isDeviceTokenPresent(String deviceId) {
        return userFcmTokenRepository.existsByDeviceId(deviceId);
    }

    public String verifyTokenByDeviceId(String deviceId) {
        RestaurantUserFcmToken userFcmToken = userFcmTokenRepository.findByDeviceId(deviceId);
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