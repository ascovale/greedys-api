package com.application.web.dto.get;

import java.time.LocalTime;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.model.reservation.Slot;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SlotDTO", description = "DTO for slot details")
public class SlotDTO {
		
	Long id;

	LocalTime start;

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