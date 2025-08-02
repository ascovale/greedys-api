package com.application.common.web.dto.restaurant;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.format.annotation.DateTimeFormat;

import com.application.common.persistence.model.reservation.Service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ServiceDTO", description = "DTO for service details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO {
    private Long id;
    private String name;
    private long restaurantId;
    private Collection<ServiceTypeDto> serviceType;
    private String info;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;
    private LocalDate validTo;
    private boolean active;


    public ServiceDTO(Service service){
        this.id = service.getId();
        this.name = service.getName();
        this.restaurantId = service.getRestaurant().getId();
        this.serviceType = service.getServiceTypes().stream().map(ServiceTypeDto::new).toList();
        this.info = service.getInfo();
        this.validFrom = service.getValidFrom();
        this.validTo = service.getValidTo();
        this.active = service.isActive();
        
    }
}
