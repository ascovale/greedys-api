package com.application.persistence.dao.user;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.user.User;


@Repository
public interface UserDAO extends  JpaRepository<User, Long>{

	public User findByEmail(String email);

	@Query("SELECT u.restaurant FROM RestaurantUser u WHERE u.user.id = :id")
	public Collection<Restaurant> getRestaurants(Long id);
}