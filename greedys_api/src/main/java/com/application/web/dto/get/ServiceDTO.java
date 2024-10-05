package com.application.web.dto.get;

import com.application.persistence.model.reservation.Service;
import com.application.web.dto.ServiceTypeDto;
import java.util.Collection;

public class ServiceDTO {
    long id;
    String name;
    long restaurantId;
    Collection<ServiceTypeDto> serviceType;
    String info;

    public ServiceDTO(Service service){
        this.id = service.getId();
        this.name = service.getName();
        this.restaurantId = service.getRestaurant().getId();
        this.serviceType = service.getServiceType().stream().map(ServiceTypeDto::new).toList();
        this.info = service.getInfo();
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
