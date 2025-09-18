package com.application.common.web.dto.reservations;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.common.controller.validators.PasswordMatches;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PasswordMatches
@Schema(name = "RestaurantReservationDto", description = "DTO for restaurant reservation details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantReservationDto {
	@NotNull
	private Integer pax;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate date;
	private Long company;// CompanyUser companyUser;
	private Long restaurant;// Restaurant restaurant;
	private Long menu;// Menu menu;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime time;// Time
}
