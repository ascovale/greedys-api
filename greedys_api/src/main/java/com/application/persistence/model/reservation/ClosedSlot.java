package com.application.persistence.model.reservation;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "closed_slot")
public class ClosedSlot {
    
    @Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id")
    Slot slot;

    LocalDate date;

    public Long getId() {
        return id;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate closeDate) {
        this.date = closeDate;
    }
    
}
