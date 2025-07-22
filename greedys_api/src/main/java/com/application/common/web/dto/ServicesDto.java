package com.application.common.web.dto;

import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ServicesDto", description = "DTO for services details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicesDto {
	String name;
	LocalTime open;
	LocalTime close;
	LocalTime slotInterval;
	LocalTime timeReservation;
	Boolean dayAfter;
	Long serviceType;
	List<String> weekdays;
	Long restaurant;
}