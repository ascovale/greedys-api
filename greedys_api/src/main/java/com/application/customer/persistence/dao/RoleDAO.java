package com.application.customer.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.customer.persistence.model.Role;
@Repository
public interface RoleDAO extends JpaRepository<Role, Long> {

    Role findByName(String name);
    @Override
    void delete(Role role);

}
