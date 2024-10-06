package com.application.web.dto.post;

import java.time.LocalTime;

import com.application.mapper.Mapper.Weekday;
import com.fasterxml.jackson.annotation.JsonFormat;

public class NewSlotDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    LocalTime start;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
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
