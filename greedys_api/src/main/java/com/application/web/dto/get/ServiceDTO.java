package com.application.web.dto.get;


import java.time.LocalDate;

import com.application.persistence.model.reservation.Service;
import com.application.web.dto.ServiceTypeDto;
import java.util.Collection;

public class ServiceDTO {
    private long id;
    private String name;
    private long restaurantId;
    private Collection<ServiceTypeDto> serviceType;
    private String info;
    private LocalDate validFrom;
    private LocalDate validTo;
    private boolean active;


    public ServiceDTO(Service service){
        this.id = service.getId();
        this.name = service.getName();
        this.restaurantId = service.getRestaurant().getId();
        this.serviceType = service.getServiceType().stream().map(ServiceTypeDto::new).toList();
        this.info = service.getInfo();
        this.validFrom = service.getValidFrom();
        this.validTo = service.getValidTo();
        this.active = service.isActive();
        
    }

    public boolean isActive() {
        return active;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    public Collection<ServiceTypeDto> getServiceType() {
        return serviceType;
    }

    public String getInfo() {
        return info;
    }
}
