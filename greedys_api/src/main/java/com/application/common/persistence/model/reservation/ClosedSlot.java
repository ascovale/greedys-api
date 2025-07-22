package com.application.common.persistence.model.reservation;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "closed_slot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClosedSlot {
    
    @Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id")
    Slot slot;

    LocalDate date;
    
}
