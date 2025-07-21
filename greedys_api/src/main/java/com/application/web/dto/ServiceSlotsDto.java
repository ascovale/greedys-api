package com.application.web.dto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.web.dto.get.SlotDTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ServiceSlotsDto", description = "DTO for service slots details")
public class ServiceSlotsDto {
	LocalTime open;
	LocalTime close;
	String name;
	Weekday weekday;
	String serviceType;
	List<SlotDTO> slots;
	
	public ServiceSlotsDto(Service service) {
		this.name = service.getName();
		this.serviceType = service.getServiceTypes().stream()
			.map(ServiceType::getName)
			.collect(Collectors.joining(", ")); // Aggiungi questa linea
		List<SlotDTO> slotDtos = new ArrayList<>();
		for (Slot slot : service.getSlots()) {
			SlotDTO slotDto = new SlotDTO(slot);
			System.out.println("<<<<<<SLOT   "+name + " " + slotDto.getId());
			System.out.println("SLOT   "+name + " " + slotDto.getStart() + " " + slotDto.getEnd());
		}
		this.slots = slotDtos;
	}

	public ServiceSlotsDto() {
    }

    public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalTime getOpen() {
		return open;
	}

	public void setOpen(LocalTime open) {
		this.open = open;
	}

	public LocalTime getClose() {
		return close;
	}

	public void setClose(LocalTime close) {
		this.close = close;
	}

	public Weekday getWeekday() {
		return weekday;
	}

	public void setWeekday(Weekday weekday) {
		this.weekday = weekday;
	}
		
	public List<SlotDTO> getSlots() {
		return slots;
	}

	public void setSlots(List<SlotDTO> slots) {
		this.slots = slots;
	}

	

}