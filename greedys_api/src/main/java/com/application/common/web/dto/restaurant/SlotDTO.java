package com.application.common.web.dto.restaurant;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Slot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "SlotDTO", description = "DTO for slot details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotDTO {
		
	Long id;

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "14:30")
    LocalTime start;

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "15:30")
    LocalTime end;
    
    Weekday weekday;
    ServiceDTO service;

    public SlotDTO(Slot slot) {
        this.id = slot.getId();
        this.start = slot.getStart();
        this.end = slot.getEnd();
        this.weekday = slot.getWeekday();
        this.service = new ServiceDTO(slot.getService());
    }
}
