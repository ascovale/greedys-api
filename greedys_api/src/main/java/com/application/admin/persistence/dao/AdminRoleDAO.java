package com.application.admin.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.admin.persistence.model.AdminRole;
@Repository
public interface AdminRoleDAO extends JpaRepository<AdminRole, Long> {

    AdminRole findByName(String name);
    @Override
    void delete(AdminRole adminrole);

}
