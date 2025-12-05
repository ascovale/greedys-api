package com.application.restaurant.service.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per la risposta dopo un upload di immagine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ImageUploadResponse", description = "Risposta dopo il caricamento di un'immagine")
public class ImageUploadResponse {

    @Schema(description = "ID dell'immagine creata", example = "1")
    private Long id;

    @Schema(description = "Tipo di immagine", example = "GALLERY")
    private String imageType;

    @Schema(description = "Path relativo dell'immagine", example = "123/abc-123.jpg")
    private String filePath;

    @Schema(description = "Path relativo della thumbnail", example = "123/abc-123_thumb.jpg")
    private String thumbnailPath;

    @Schema(description = "Larghezza finale in pixel", example = "1920")
    private Integer width;

    @Schema(description = "Altezza finale in pixel", example = "1080")
    private Integer height;

    @Schema(description = "Dimensione file in bytes", example = "245678")
    private Long fileSize;

    @Schema(description = "Dimensione file leggibile", example = "240.0 KB")
    private String fileSizeReadable;

    @Schema(description = "Messaggio di conferma", example = "Immagine caricata con successo")
    private String message;
}
