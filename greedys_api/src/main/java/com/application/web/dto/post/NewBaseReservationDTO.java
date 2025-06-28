package com.application.web.dto.post;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NewBaseReservationDTO", description = "Base DTO for creating a reservation")
public abstract class NewBaseReservationDTO {
    @Schema(description = "Slot ID for the reservation")
    private Long idSlot;
    @Schema(description = "Number of adults")
    private Integer pax;
    @Schema(description = "Number of kids")
    private Integer kids = 0;
    @Schema(description = "Notes for the reservation")
    private String notes;
    @Schema(description = "Reservation date")
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate reservationDay;

    public Long getIdSlot() {
        return idSlot;
    }
    public void setIdSlot(Long idSlot) {
        this.idSlot = idSlot;
    }
    public Integer getPax() {
        return pax;
    }
    public void setPax(Integer pax) {
        this.pax = pax;
    }
    public Integer getKids() {
        return kids;
    }
    public void setKids(Integer kids) {
        this.kids = kids;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public LocalDate getReservationDay() {
        return reservationDay;
    }
    public void setReservationDay(LocalDate reservationDay) {
        this.reservationDay = reservationDay;
    }
}
