package com.application.web.dto.post;

public class NewTableDTO {

    private String name;

    private Long roomId;

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
    
}
