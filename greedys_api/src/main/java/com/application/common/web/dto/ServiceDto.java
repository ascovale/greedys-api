package com.application.common.web.dto;

import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ServiceDto", description = "DTO for service details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {
	LocalTime open;
	LocalTime close;
	String name;
	Weekday weekday;
	Set<ServiceTypeDto> serviceTypes;
	
	public ServiceDto(Service service) {
		this.name = service.getName();
		this.serviceTypes = service.getServiceTypes().stream()
			.map(ServiceTypeDto::new)
			.collect(Collectors.toSet());
	}
}