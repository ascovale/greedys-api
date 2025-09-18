package com.application.common.web.dto.restaurant;

import java.time.LocalDate;
import java.util.List;

import com.application.common.persistence.model.reservation.Reservation;
import com.application.restaurant.persistence.model.Restaurant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RestaurantServizioDto", description = "DTO for restaurant service details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantServizioDto {
	private Long id;
	private String name;
	private LocalDate startDate;
	private LocalDate endDate;
	private String description;
	private List<Reservation> reservations;
	private Restaurant restaurant;
}
