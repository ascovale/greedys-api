package com.application.restaurant.web.post;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewReservationDTO", description = "DTO for creating a new reservation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewReservationDTO {
	
	private Long idSlot;
	private Integer pax;
	@Builder.Default
	private Integer kids=0;
	private String notes;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
	private LocalDate reservationDay; 
    private Long restaurant_id;
	private Long user_id;

	public Boolean isAnonymous() {
		return user_id == null;
	}

	public Long getUser_id() {
		if (user_id == null) {
			throw new IllegalArgumentException("User id is null, the reservation is anonymous.");
		}
		return user_id;
	}
}

