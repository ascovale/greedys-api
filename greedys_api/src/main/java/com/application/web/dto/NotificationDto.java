package com.application.web.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.application.persistence.model.user.Notification;
import com.application.persistence.model.user.Notification.Type;

public class NotificationDto {
	
	private Long id;
	private Type type;
	private Long idUser;
	private Long idReservation;
	private Boolean read;
	private String text;
	private Timestamp creationTime;
	
	
	
	public NotificationDto(Long id, Type type, Long idUser, Long idReservation, Boolean read, String text,
			Timestamp creationTime) {
		this.id = id;
		this.type = type;
		this.idUser = idUser;
		this.idReservation = idReservation;
		this.read = !read;
		this.text = text;
		this.creationTime = creationTime;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public Long getIdUser() {
		return idUser;
	}
	public void setIdUser(Long idUser) {
		this.idUser = idUser;
	}
	public Long getIdReservation() {
		return idReservation;
	}
	public void setIdReservation(Long idReservation) {
		this.idReservation = idReservation;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Timestamp getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}
	
	public Boolean getRead() {
		return read;
	}
	public void setRead(Boolean read) {
		this.read = read;
	}
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("NotificationDto [id=").append(id).append(", message=").append(text)
				.append("]");
		return builder.toString();
	}
	public static List<NotificationDto> toDto(List<Notification> notifications) {
		ArrayList<NotificationDto> list = new ArrayList<NotificationDto>();
		for(Notification n : notifications) {
			list.add(new NotificationDto(
					n.getId(),
					n.getType(),
					n.getClientUser().getId(), 
					null,//n.getReservation().getId(),
					n.getUnopened(), 
					n.getText(),
					n.getCreationTime()
					));
		}
 		return list;
	}
	
	public static NotificationDto toDto(Notification n) {
		return new NotificationDto(
					n.getId(),
					n.getType(),
					n.getClientUser().getId(), 
					null,//n.getReservation().getId(),
					n.getUnopened(), 
					n.getText(),
					n.getCreationTime()
					);
		
	}


}
