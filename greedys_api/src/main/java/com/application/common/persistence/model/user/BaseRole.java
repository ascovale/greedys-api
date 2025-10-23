package com.application.common.persistence.model.user;

import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
public interface BaseRole<T extends BasePrivilege> {
    String getName();
    void setPrivileges(List<T> privileges);
    void addPrivilege(T privilege);
    List<T> getPrivileges();    
}
