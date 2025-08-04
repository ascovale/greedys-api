package com.application.persistence.model.restaurant.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "restaurant_user_options")
public class RUserOptions {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    

 
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
