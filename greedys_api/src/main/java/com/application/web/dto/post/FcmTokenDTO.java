package com.application.web.dto.post;

import java.time.LocalDateTime;

import com.application.persistence.model.fcm.AFcmToken;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FcmTokenDTO {

    private String fcmToken;
    private LocalDateTime createdAt;
    private String deviceId;

    public FcmTokenDTO(AFcmToken token) {
        this.fcmToken = token.getFcmToken();
        this.createdAt = token.getCreatedAt();
        this.deviceId = token.getDeviceId();
    }

}