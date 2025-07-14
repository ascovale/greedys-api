package com.application.persistence.model.restaurant.user;

import java.util.HashMap;
import java.util.Map;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
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
