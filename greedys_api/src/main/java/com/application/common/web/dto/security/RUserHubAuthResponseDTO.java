package com.application.common.web.dto.security;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class RUserHubAuthResponseDTO {
    private String token;
    private List<RestaurantInfo> restaurants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantInfo {
        private Long id;
        private String name;
    }
}
