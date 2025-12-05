package com.application.agency.service.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per la risposta dopo un upload del logo dell'agenzia
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AgencyLogoUploadResponse", description = "Risposta dopo il caricamento del logo")
public class AgencyLogoUploadResponse {

    @Schema(description = "ID del logo creato", example = "1")
    private Long id;

    @Schema(description = "Path relativo del logo", example = "agencies/123/abc-123.png")
    private String filePath;

    @Schema(description = "Path relativo della thumbnail", example = "agencies/123/abc-123_thumb.png")
    private String thumbnailPath;

    @Schema(description = "Larghezza finale in pixel", example = "400")
    private Integer width;

    @Schema(description = "Altezza finale in pixel", example = "400")
    private Integer height;

    @Schema(description = "Dimensione file in bytes", example = "125678")
    private Long fileSize;

    @Schema(description = "Dimensione file leggibile", example = "122.7 KB")
    private String fileSizeReadable;

    @Schema(description = "Messaggio di conferma", example = "Logo caricato con successo")
    private String message;
}
