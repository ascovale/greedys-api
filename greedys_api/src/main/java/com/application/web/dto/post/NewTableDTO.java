package com.application.web.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NewTableDTO", description = "DTO for creating a new table")
public class NewTableDTO {
    private Long idRestaurant;

    private String name;

    private Long roomId;

    private int capacity;

    private int positionX;

    private int positionY;

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setIdRestaurant(Long idRestaurant) {
        this.idRestaurant = idRestaurant;
    }

    public Long getIdRestaurant() {
        return idRestaurant;
    }
    
}
