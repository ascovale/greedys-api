package com.application.admin.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.admin.model.AdminPrivilege;
@Repository
public interface AdminPrivilegeDAO extends JpaRepository<AdminPrivilege, Long> {

    AdminPrivilege findByName(String name);

    @Override
    void delete(AdminPrivilege privilege);

}
