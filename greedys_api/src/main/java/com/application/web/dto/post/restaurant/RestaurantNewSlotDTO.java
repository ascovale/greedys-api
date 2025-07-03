package com.application.web.dto.post.restaurant;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.mapper.Mapper.Weekday;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestaurantNewSlotDTO", description = "DTO for creating a new restaurant slot")
public class RestaurantNewSlotDTO {


    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "14:30")
    LocalTime start;

    @DateTimeFormat(pattern = "HH:mm")
    @Schema(type = "string", format = "time", example = "15:30")
    LocalTime end;

    Weekday weekday;
    Long serviceId;

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

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }
    
}
