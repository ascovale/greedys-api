package com.application.common.persistence.model.reservation;

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
	
}
