package com.application.persistence.model.systemconfig;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SetupConfig {
    @Id
    private Long id;
    private boolean alreadySetup=false;
    private boolean dataUploaded=false;

    public boolean isDataUploaded() {
        return dataUploaded;
    }

    public void setDataUploaded(boolean dataUploaded) {
        this.dataUploaded = dataUploaded;
    }

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