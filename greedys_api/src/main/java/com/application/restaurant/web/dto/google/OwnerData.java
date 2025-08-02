package com.application.restaurant.web.dto.google;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Owner data from Google OAuth
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerData {
    private String email;
    private String name;
    private String profilePicture;
    private String verificationStatus;
    private String googleId;
    private String locale;
    private LocalDateTime verificationDate;
}
