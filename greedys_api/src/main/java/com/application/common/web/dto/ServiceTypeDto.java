package com.application.common.web.dto;

import com.application.common.persistence.model.reservation.ServiceType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ServiceTypeDto", description = "DTO for service type details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTypeDto {
	Long id;
	String name;

	public ServiceTypeDto(ServiceType serviceType) {
		this.id = serviceType.getId();
		this.name = serviceType.getName();
	}

	public ServiceTypeDto(String name) {
		this.name = name;
	}
}
