package com.application.restaurant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RUserAuthenticationDetails {
    private final boolean bypassPasswordCheck;
    private final Long restaurantId;
    private final String email;
}