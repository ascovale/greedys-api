package com.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RUserFcmTokenDAO;
import com.application.persistence.model.fcm.RUserFcmToken;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.web.dto.post.FcmTokenDTO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class RUserFcmTokenService {
    private final RUserFcmTokenDAO userFcmTokenRepository;

    public RUserFcmTokenService(RUserFcmTokenDAO userFcmTokenRepository, EntityManager entityManager) {
        this.userFcmTokenRepository = userFcmTokenRepository;
    }

    public void saveUserFcmToken(FcmTokenDTO userFcmTokenDTO) {
        RUser RUser = (RUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = RUser.getId();
        String token = userFcmTokenDTO.getFcmToken();
        String deviceId = userFcmTokenDTO.getDeviceId();
        if (isDeviceTokenPresent(userId, deviceId)) {
            RUserFcmToken existingToken = findByUserIdAndDeviceId(userId, deviceId);
            userFcmTokenRepository.delete(existingToken);
        }
        RUserFcmToken userFcmToken = new RUserFcmToken(RUser, token, deviceId);
        userFcmTokenRepository.save(userFcmToken);
    }
    
    public List<String> getTokensByRUserId(Long id) {
        return userFcmTokenRepository.findByRUserId(id).stream()
                .map(RUserFcmToken::getFcmToken)
                .toList();
    }

    public RUserFcmToken getTokenByDeviceId(String deviceId) {
        return userFcmTokenRepository.findByDeviceId(deviceId);
    }

    public boolean isDeviceTokenPresent(Long userId, String deviceId) {
        return userFcmTokenRepository.existsByRUserIdAndDeviceId(userId, deviceId);
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

    public RUserFcmToken findByUserIdAndDeviceId(Long userId, String deviceId) {
        return userFcmTokenRepository.findByRUserIdAndDeviceId(userId, deviceId);
    }
}