package com.application.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
(name = "AllergyDTO", description = "DTO for creating an allergy")
public class AllergyDTO {
    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
