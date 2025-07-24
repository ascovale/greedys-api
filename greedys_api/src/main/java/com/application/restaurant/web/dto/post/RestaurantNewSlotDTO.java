package com.application.restaurant.web.dto.post;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.common.persistence.mapper.Mapper.Weekday;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "RestaurantNewSlotDTO", description = "DTO for creating a new restaurant slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantNewSlotDTO {

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "14:30")
    private LocalTime start;

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "15:30")
    private LocalTime end;

    private Weekday weekday;
    private Long serviceId;
}
