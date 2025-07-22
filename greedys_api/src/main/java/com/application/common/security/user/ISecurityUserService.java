package com.application.common.security.user;

public interface ISecurityUserService {

    String validatePasswordResetToken(String token);

}
