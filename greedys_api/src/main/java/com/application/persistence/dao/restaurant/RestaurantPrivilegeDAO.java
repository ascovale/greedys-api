package com.application.persistence.dao.restaurant;

import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.RestaurantPrivilege;

@Repository
public interface RestaurantPrivilegeDAO {
    public RestaurantPrivilege findByName(String name);
}
