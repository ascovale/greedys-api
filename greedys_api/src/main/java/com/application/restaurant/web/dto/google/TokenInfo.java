package com.application.restaurant.web.dto.google;

import lombok.Data;

/**
 * Google token information from tokeninfo endpoint
 */
@Data
public class TokenInfo {
    private String audience;
    private String scope;
    private String userId;
    private String expiresIn;
    private String accessType;
}
