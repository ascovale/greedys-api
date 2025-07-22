package com.application.common.web.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "NotificationDto", description = "DTO for notification details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
	
	private Long id;
	private Long idUser;
	private Boolean read;
	private String text;
	private Instant creationTime;
}
