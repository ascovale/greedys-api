package com.application.admin.web.dto.post;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "AdminNewServiceDTO", description = "DTO for creating a new admin service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNewServiceDTO {

	private String name;
	private Long restaurant;
	private Long serviceType;
	private String info;
	private LocalDate validFrom;
	private LocalDate validTo;
}