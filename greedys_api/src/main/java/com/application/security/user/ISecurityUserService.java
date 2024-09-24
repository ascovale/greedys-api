package com.application.security.user;

public interface ISecurityUserService {

    String validatePasswordResetToken(long id, String token);

}
