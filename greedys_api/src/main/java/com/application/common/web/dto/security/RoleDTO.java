package com.application.common.web.dto.security;

import java.util.List;

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
@Schema(name = "RoleDTO", description = "DTO for role details")
public class RoleDTO {

    @Schema(description = "ID of the role", example = "1")
    private Long id;

    @Schema(description = "Name of the role", example = "ADMIN")
    @NotNull
    @Size(min = 1, message = "Role name cannot be empty")
    private String name;

    @Schema(description = "List of privileges associated with this role")
    private List<PrivilegeDTO> privileges;
}
