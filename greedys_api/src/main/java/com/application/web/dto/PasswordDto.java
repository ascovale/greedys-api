package com.application.web.dto;

import com.application.controller.Validators.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PasswordDto", description = "DTO for password change requests")
public class PasswordDto {

    private String oldPassword;

    @ValidPassword
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
