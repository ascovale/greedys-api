package com.application.common.web.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PrivilegeDTO", description = "DTO for privilege details")
public class PrivilegeDTO {

    @Schema(description = "ID of the privilege", example = "1")
    private Long id;

    @Schema(description = "Name of the privilege", example = "READ_USERS")
    @NotNull
    @Size(min = 1, message = "Privilege name cannot be empty")
    private String name;
}
