package com.application.restaurant.service.image.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le informazioni di un'immagine della galleria del ristorante
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RestaurantGalleryImageDTO", description = "Dettagli di un'immagine della galleria")
public class RestaurantGalleryImageDTO {

    @Schema(description = "ID univoco dell'immagine", example = "1")
    private Long id;

    @Schema(description = "ID del ristorante proprietario", example = "123")
    private Long restaurantId;

    @Schema(description = "Tipo di immagine", example = "GALLERY", allowableValues = {"LOGO", "COVER", "GALLERY"})
    private String imageType;

    @Schema(description = "Nome file originale", example = "ristorante_vista.jpg")
    private String originalFilename;

    @Schema(description = "MIME type", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "Dimensione file in bytes", example = "245678")
    private Long fileSize;

    @Schema(description = "Dimensione file leggibile", example = "240.0 KB")
    private String fileSizeReadable;

    @Schema(description = "Larghezza in pixel", example = "1920")
    private Integer width;

    @Schema(description = "Altezza in pixel", example = "1080")
    private Integer height;

    @Schema(description = "Aspect ratio", example = "1.78")
    private Double aspectRatio;

    @Schema(description = "Path relativo dell'immagine", example = "123/abc-123.jpg")
    private String filePath;

    @Schema(description = "Path relativo della thumbnail", example = "123/abc-123_thumb.jpg")
    private String thumbnailPath;

    @Schema(description = "Titolo/descrizione dell'immagine", example = "Vista panoramica del ristorante")
    private String title;

    @Schema(description = "Testo alternativo per accessibilità", example = "Vista esterna del ristorante")
    private String altText;

    @Schema(description = "Ordine di visualizzazione", example = "1")
    private Integer displayOrder;

    @Schema(description = "Flag immagine in evidenza", example = "false")
    private Boolean isFeatured;

    @Schema(description = "Data di creazione")
    private Instant createdAt;

    // ==================== COMPUTED URLs ====================
    
    /**
     * Genera l'URL completo dell'immagine principale.
     * Il baseUrl può essere impostato dal controller.
     */
    public String getImageUrl(String baseUrl) {
        return baseUrl + "/api/v1/restaurant/images/file/" + filePath;
    }

    /**
     * Genera l'URL completo della thumbnail.
     * Il baseUrl può essere impostato dal controller.
     */
    public String getThumbnailUrl(String baseUrl) {
        if (thumbnailPath == null) return null;
        return baseUrl + "/api/v1/restaurant/images/file/" + thumbnailPath;
    }
}
