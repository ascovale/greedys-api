package com.application.common.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewAllergyDTO", description = "DTO for creating a new allergy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewAllergyDTO {
    private String name;
    private String description;
}
