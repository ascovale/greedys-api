package com.application.web.dto.get;

public class RoomDTO {

    private Long id;
    private String name;

    public RoomDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
}
