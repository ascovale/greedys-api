package com.application.service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.Restaurant.RestaurantUserDAO;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;

@Service
@Transactional
public class RestaurantUserService {

    @Autowired
	private RestaurantUserDAO ruDAO;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestaurantRoleService roleService;


    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " " + restaurantUserDTO.getUserId());
        RestaurantUser ru = new RestaurantUser();
		ru.setRestaurant(restaurantService.findById(restaurantUserDTO.getRestaurantId()));
		ru.setUser(userService.getReference(restaurantUserDTO.getUserId()));
        ruDAO.save(ru);
        return ru;
    }

    public void acceptUser(Long id) {
        // TODO Auto-generated method stub
        ruDAO.acceptUser(id);
    }

    public Collection<RestaurantUserDTO> getRestaurantUsers(Long id) {
        return ruDAO.findByRestaurantId(id).stream().map(user -> new RestaurantUserDTO(user)).toList();
    }


}
