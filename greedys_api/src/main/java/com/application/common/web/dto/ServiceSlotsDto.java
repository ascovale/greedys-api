package com.application.common.web.dto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.ServiceType;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.get.SlotDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ServiceSlotsDto", description = "DTO for service slots details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}