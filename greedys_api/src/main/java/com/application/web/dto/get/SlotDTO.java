package com.application.web.dto.get;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.model.reservation.Slot;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SlotDTO", description = "DTO for slot details")
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
    
    public Long getId() {
        return id;
    }

    public LocalTime getStart() {
        return start;
    }

    public void setStart(LocalTime start) {
        this.start = start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public void setEnd(LocalTime end) {
        this.end = end;
    }

    public Weekday getWeekday() {
        return weekday;
    }

    public void setWeekday(Weekday weekday) {
        this.weekday = weekday;
    }

    public ServiceDTO getService() {
        return service;
    }

    public void setService(ServiceDTO serviceId) {
        this.service = serviceId;
    }
}