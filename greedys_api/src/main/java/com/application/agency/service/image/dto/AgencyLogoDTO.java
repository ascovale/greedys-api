package com.application.agency.service.image.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le informazioni del logo dell'agenzia
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AgencyLogoDTO", description = "Dettagli del logo dell'agenzia")
public class AgencyLogoDTO {

    @Schema(description = "ID univoco del logo", example = "1")
    private Long id;

    @Schema(description = "ID dell'agenzia proprietaria", example = "123")
    private Long agencyId;

    @Schema(description = "Nome file originale", example = "logo_agenzia.png")
    private String originalFilename;

    @Schema(description = "MIME type", example = "image/png")
    private String mimeType;

    @Schema(description = "Dimensione file in bytes", example = "125678")
    private Long fileSize;

    @Schema(description = "Dimensione file leggibile", example = "122.7 KB")
    private String fileSizeReadable;

    @Schema(description = "Larghezza in pixel", example = "400")
    private Integer width;

    @Schema(description = "Altezza in pixel", example = "400")
    private Integer height;

    @Schema(description = "Aspect ratio", example = "1.0")
    private Double aspectRatio;

    @Schema(description = "Path relativo del logo", example = "agencies/123/abc-123.png")
    private String filePath;

    @Schema(description = "Path relativo della thumbnail", example = "agencies/123/abc-123_thumb.png")
    private String thumbnailPath;

    @Schema(description = "Testo alternativo per accessibilità", example = "Logo Agenzia Viaggi ABC")
    private String altText;

    @Schema(description = "Data di creazione")
    private Instant createdAt;

    /**
     * Genera l'URL completo del logo.
     */
    public String getLogoUrl(String baseUrl) {
        return baseUrl + "/api/v1/agency/images/file/" + filePath;
    }

    /**
     * Genera l'URL completo della thumbnail.
     */
    public String getThumbnailUrl(String baseUrl) {
        if (thumbnailPath == null) return null;
        return baseUrl + "/api/v1/agency/images/file/" + thumbnailPath;
    }
}
