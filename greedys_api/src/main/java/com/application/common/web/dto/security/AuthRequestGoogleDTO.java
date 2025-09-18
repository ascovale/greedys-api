package com.application.common.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "AuthRequestGoogleDTO", description = "DTO for Google authentication requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestGoogleDTO {
    private String token;
}
