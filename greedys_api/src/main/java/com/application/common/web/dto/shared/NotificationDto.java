package com.application.common.web.dto.shared;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NotificationDto", description = "DTO for notification details")
public class NotificationDto {
	
	private Long id;
	private Long idUser;
	private Boolean read;
	private String text;
	private Instant creationTime;

	
	public NotificationDto(Long id, Long idUser, Boolean read, String text,
			Instant creationTime) {
		this.id = id;
		this.idUser = idUser;
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
	public Long getIdUser() {
		return idUser;
	}
	public void setIdUser(Long idUser) {
		this.idUser = idUser;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Instant getCreationTime() {
		return creationTime;
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
}
