package com.application.restaurant.web.dto.services;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.common.persistence.mapper.Mapper.Weekday;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NewSlotDTO", description = "DTO for creating a new slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewSlotDTO {

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "14:30")
    LocalTime start;

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "15:30")
    LocalTime end;

    Weekday weekday;
    Long serviceId;
}
