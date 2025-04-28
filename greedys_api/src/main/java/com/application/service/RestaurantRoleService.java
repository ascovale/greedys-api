package com.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantRoleDAO;
import com.application.persistence.model.restaurant.user.RestaurantRole;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class RestaurantRoleService {
    
    @Autowired
    private RestaurantRoleDAO roleDAO;

    public RestaurantRole findById(Long id) {
        return roleDAO.findById(id).orElseThrow(() -> new EntityNotFoundException("RestaurantRole not found with id: " + id));
    }
}
