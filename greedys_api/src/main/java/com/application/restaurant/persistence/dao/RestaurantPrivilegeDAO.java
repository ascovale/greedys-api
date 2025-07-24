package com.application.restaurant.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.restaurant.persistence.model.user.RestaurantPrivilege;

@Repository
public interface RestaurantPrivilegeDAO extends JpaRepository<RestaurantPrivilege, Long>{

    public RestaurantPrivilege findByName(String name);
}
