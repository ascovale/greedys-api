package com.application.restaurant.web.dto.google;

import lombok.Data;

/**
 * Google user information from OAuth userinfo endpoint
 */
@Data
public class GoogleUserInfo {
    private String id;
    private String email;
    private String name;
    private String picture;
    private String locale;
}
