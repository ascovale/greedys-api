package com.application.persistence.dao.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.admin.AdminPrivilege;
@Repository
public interface AdminPrivilegeDAO extends JpaRepository<AdminPrivilege, Long> {

    AdminPrivilege findByName(String name);

    @Override
    void delete(AdminPrivilege privilege);

}
