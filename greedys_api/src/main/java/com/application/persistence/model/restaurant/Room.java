package com.application.persistence.model.restaurant;

import com.application.persistence.model.restaurant.Table ;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;


@Entity
@jakarta.persistence.Table(name = "room")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "room")
    private List<Table> tables;


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public List<Table> getTables() {
        return tables;
    }

    public Long getId() {
        return id;
    }

}
