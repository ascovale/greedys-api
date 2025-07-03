package com.application.web.dto.post.admin;

import com.application.controller.validators.ValidEmail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmailRequestDTO", description = "DTO for email requests")
public class EmailRequestDTO { // Renamed from EmailRequestDTO to EmailRequestDto
    @ValidEmail
    private String email;
    private String subject;
    private String message;

    public EmailRequestDTO(String email, String subject, String message) { // Updated constructor name
        this.email = email;
        this.subject = subject;
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}