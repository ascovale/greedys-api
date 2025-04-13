package com.application.web.dto.post;

import java.util.List;

public class NewMenuDTO {

    private String name;
    private String description;
    private Float price;
    private List<Long> ServiceIds;

    
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Float getPrice() {
        return price;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public List<Long> getServiceIds() {
        return ServiceIds;
    }

    public void setServiceIds(List<Long> serviceIds) {
        this.ServiceIds = serviceIds;
    }
    
    
}
