package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RUserFcmTokenDAO;
import com.application.persistence.model.notification.RUserFcmToken;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.web.dto.post.UserFcmTokenDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class RUserFcmTokenService {
    private final RUserFcmTokenDAO userFcmTokenRepository;
    private final EntityManager entityManager;

    public RUserFcmTokenService(RUserFcmTokenDAO userFcmTokenRepository, EntityManager entityManager) {
        this.userFcmTokenRepository = userFcmTokenRepository;
        this.entityManager = entityManager;
    }

    public void saveUserFcmToken(UserFcmTokenDTO userFcmTokenDTO) {
        RUser RUser = entityManager.getReference(RUser.class, userFcmTokenDTO.getUserId());
        String token = userFcmTokenDTO.getFcmToken();
        String deviceId = userFcmTokenDTO.getDeviceId();

        RUserFcmToken userFcmToken = new RUserFcmToken(RUser, token, deviceId);
        userFcmTokenRepository.save(userFcmToken);
    }
    
    public List<RUserFcmToken> getTokensByRUserId(Long id) {
        return userFcmTokenRepository.findByRUserId(id);
    }

    public RUserFcmToken getTokenByDeviceId(String deviceId) {
        return userFcmTokenRepository.findByDeviceId(deviceId);
    }

    public boolean isDeviceTokenPresent(String deviceId) {
        return userFcmTokenRepository.existsByDeviceId(deviceId);
    }

    public String verifyTokenByDeviceId(String deviceId) {
        RUserFcmToken userFcmToken = userFcmTokenRepository.findByDeviceId(deviceId);
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