package com.application.persistence.model.systemconfig;


import org.springframework.data.annotation.Id;

import jakarta.persistence.Entity;

@Entity
public class SetupConfig {
    @Id
    private Long id;
    private boolean alreadySetup;

    // getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isAlreadySetup() {
        return alreadySetup;
    }

    public void setAlreadySetup(boolean alreadySetup) {
        this.alreadySetup = alreadySetup;
    }
}