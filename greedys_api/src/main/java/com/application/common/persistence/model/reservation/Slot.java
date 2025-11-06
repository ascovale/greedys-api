package com.application.common.persistence.model.reservation;

import java.time.LocalDate;
import java.time.LocalTime;

import com.application.common.persistence.mapper.Mapper.Weekday;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "slot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slot {
		
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	Long id;
	@Schema(type = "string", format = "time", example = "14:30")
	@Column(name = "start_time")
	LocalTime start;
	@Schema(type = "string", format = "time", example = "15:30")
	@Column(name = "end_time")
	LocalTime end;
	@Column(name="weekday")
	@Enumerated(EnumType.STRING)
	Weekday weekday;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id")
	Service service;
	
	@Builder.Default
	private Boolean deleted = false;
	
	/**
	 * Data da cui questo slot è valido (inclusa)
	 */
	@Column(name = "valid_from")
	@Builder.Default
	private LocalDate validFrom = LocalDate.of(2024, 1, 1);
	
	/**
	 * Data fino a cui questo slot è valido (inclusa)
	 */
	@Column(name = "valid_to")
	@Builder.Default
	private LocalDate validTo = LocalDate.of(2099, 12, 31);
	
	/**
	 * Se true, lo slot è attivo e può ricevere nuove prenotazioni
	 */
	@Column(name = "active")
	@Builder.Default
	private Boolean active = true;
	
	/**
	 * ID dello slot che sostituisce questo (quando viene modificato)
	 */
	@Column(name = "superseded_by")
	private Long supersededBy;
	
	/**
	 * Policy per gestire le modifiche quando ci sono prenotazioni future
	 */
	@Column(name = "change_policy")
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private SlotChangePolicy changePolicy = SlotChangePolicy.HARD_CUT;
	
}
