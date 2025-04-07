package com.application.security.user;

public interface ISecurityUserService {

    String validatePasswordResetToken(String token);

}
