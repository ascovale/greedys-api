package com.application.common.web.dto.get;

import com.application.customer.persistence.model.Allergy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "AllergyDTO", description = "DTO for allergy details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
