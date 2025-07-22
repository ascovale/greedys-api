package com.application.common.web.dto.post;

import java.time.LocalDateTime;

import com.application.common.persistence.model.fcm.AFcmToken;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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