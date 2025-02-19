package com.application.persistence.dao.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.customer.Role;
@Repository
public interface RoleDAO extends JpaRepository<Role, Long> {

    Role findByName(String name);
    @Override
    void delete(Role role);

}
