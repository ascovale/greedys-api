package com.application.restaurant.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.post.FcmTokenDTO;
import com.application.restaurant.dao.RUserFcmTokenDAO;
import com.application.restaurant.model.user.RUser;
import com.application.restaurant.model.user.RUserFcmToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RUserFcmTokenService {
    
    private final RUserFcmTokenDAO userFcmTokenRepository;

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