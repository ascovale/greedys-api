package com.application.admin.web.dto.communication;

import com.application.common.controller.validators.ValidEmail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "EmailRequestDTO", description = "DTO for email requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestDTO {
    @ValidEmail
    private String email;
    private String subject;
    private String message;
}
