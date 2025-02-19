package com.application.persistence.dao.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.customer.Privilege;
@Repository
public interface PrivilegeDAO extends JpaRepository<Privilege, Long> {

    Privilege findByName(String name);

    @Override
    void delete(Privilege privilege);

}
