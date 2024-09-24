package com.application.web.dto.get;

import com.application.persistence.model.reservation.Service;

public class ServiceDTO {
    long id;
    String name;
    long restaurantId;
    long serviceType;
    String info;

    public ServiceDTO(Service service){
        this.id = service.getId();
        this.name = service.getName();
        this.restaurantId = service.getRestaurant().getId();
        this.serviceType = service.getServiceType().getId();
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

    public long getServiceType() {
        return serviceType;
    }

    public String getInfo() {
        return info;
    }
}
