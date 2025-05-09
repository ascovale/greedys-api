package com.application.web.dto.put;

import com.application.controller.Validators.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdatePasswordDTO", description = "DTO for updating a user's password")
public class UpdatePasswordDTO {

    private String oldPassword;

    @ValidPassword
    private String newPassword;

    private String email;
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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