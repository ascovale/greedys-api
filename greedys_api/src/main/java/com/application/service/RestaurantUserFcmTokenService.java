package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantUserFcmTokenDAO;
import com.application.persistence.model.notification.RestaurantUserFcmToken;
import com.application.persistence.model.restaurant.user.RestaurantUser;
import com.application.web.dto.post.UserFcmTokenDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class RestaurantUserFcmTokenService {
    private final RestaurantUserFcmTokenDAO userFcmTokenRepository;
    private final EntityManager entityManager;

    public RestaurantUserFcmTokenService(RestaurantUserFcmTokenDAO userFcmTokenRepository, EntityManager entityManager) {
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.entityManager = entityManager;
    }

    public void saveUserFcmToken(UserFcmTokenDTO userFcmTokenDTO) {
        RestaurantUser restaurantUser = entityManager.getReference(RestaurantUser.class, userFcmTokenDTO.getUserId());
        String token = userFcmTokenDTO.getFcmToken();
        String deviceId = userFcmTokenDTO.getDeviceId();

        RestaurantUserFcmToken userFcmToken = new RestaurantUserFcmToken(restaurantUser, token, deviceId);
        userFcmTokenRepository.save(userFcmToken);
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