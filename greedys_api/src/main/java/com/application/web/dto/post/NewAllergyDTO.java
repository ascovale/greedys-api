package com.application.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NewAllergyDTO", description = "DTO for creating a new allergy")
public class NewAllergyDTO {
    private String name;
    private String description;

    public NewAllergyDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

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
