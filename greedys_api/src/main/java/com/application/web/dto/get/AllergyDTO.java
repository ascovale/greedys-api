package com.application.web.dto.get;

import com.application.persistence.model.customer.Allergy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AllergyDTO", description = "DTO for allergy details")
public class AllergyDTO {
    private Long id;
    @Schema(description = "Name of the allergy", example = "Peanut Allergy")
    private String name;
    @Schema(description = "Description of the allergy", example = "Severe allergic reaction to peanuts")
    private String description;

    public AllergyDTO(Allergy allergy) {
        this.id = allergy.getId();
        this.name = allergy.getName();
        this.description = allergy.getDescription();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
