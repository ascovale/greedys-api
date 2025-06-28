package com.application.persistence.model.user;

import java.util.List;

public interface BaseRole<T extends BasePrivilege> {
    String getName();
    void setPrivileges(List<T> privileges);
    void addPrivilege(T privilege);
    List<T> getPrivileges();    
}
