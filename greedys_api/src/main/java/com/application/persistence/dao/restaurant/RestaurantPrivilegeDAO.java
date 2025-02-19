package com.application.persistence.dao.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.user.RestaurantPrivilege;

@Repository
public interface RestaurantPrivilegeDAO extends JpaRepository<RestaurantPrivilege, Long>{

    public RestaurantPrivilege findByName(String name);
}
