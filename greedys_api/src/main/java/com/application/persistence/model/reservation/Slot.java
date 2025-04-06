package com.application.persistence.model.reservation;

import java.time.LocalTime;

import com.application.mapper.Mapper.Weekday;
import com.application.web.dto.get.LocalTimeSerializer;
import com.application.web.dto.post.LocalTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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

@Entity
@Table(name = "slot")
public class Slot {
		
	@Id		
	@GeneratedValue(strategy=GenerationType.AUTO)
	Long id;
	@JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
	@Schema(type = "string", format = "time", example = "14:30")
	LocalTime start;
	@JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
	@Schema(type = "string", format = "time", example = "15:30")
	LocalTime end;
	@Column(name="weekday")
	@Enumerated(EnumType.STRING)
	Weekday weekday;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id")
	Service service;
	private Boolean deleted = false;
	
	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public Slot() {
	}

	public Slot(LocalTime start, LocalTime end) {
		this.start = start;
		this.end = end;
    }

    // Getter and Setter for 'id'
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	// Getter and Setter for 'start'
	public LocalTime getStart() {
		return start;
	}

	public void setStart(LocalTime start) {
		this.start = start;
	}

	// Getter and Setter for 'end'
	public LocalTime getEnd() {
		return end;
	}

	public void setEnd(LocalTime end) {
		this.end = end;
	}

	// Getter and Setter for 'service'
	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Weekday getWeekday() {
		return weekday;
	}

	public void setWeekday(Weekday weekday) {
		this.weekday = weekday;
	}

    public void setDeleted(boolean b) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDeleted'");
    }
}