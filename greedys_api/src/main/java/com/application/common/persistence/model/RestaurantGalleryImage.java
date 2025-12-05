package com.application.common.persistence.model;

import java.time.Instant;

import com.application.restaurant.persistence.model.Restaurant;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ⭐ RESTAURANT GALLERY IMAGE ENTITY
 * 
 * Rappresenta un'immagine nella galleria di un ristorante.
 * Supporta sia immagini della galleria che logo del ristorante.
 * Include metadati come dimensioni originali, thumbnail e info di compressione.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "restaurant_gallery_image", indexes = {
    @Index(name = "idx_gallery_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_gallery_type", columnList = "image_type"),
    @Index(name = "idx_gallery_order", columnList = "display_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantGalleryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== RELATIONSHIP ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @JsonBackReference
    private Restaurant restaurant;

    // ==================== IMAGE TYPE ====================
    
    /**
     * Tipo di immagine: LOGO, GALLERY, COVER
     */
    public enum ImageType {
        LOGO,       // Logo del ristorante (singolo)
        COVER,      // Immagine di copertina (singola)
        GALLERY     // Immagini della galleria (multiple)
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    @Builder.Default
    private ImageType imageType = ImageType.GALLERY;

    // ==================== FILE INFO ====================
    
    /**
     * Nome file originale caricato dall'utente
     */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    
    /**
     * Nome file salvato sul server (UUID + estensione)
     */
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    /**
     * MIME type del file (es: image/jpeg, image/png, image/webp)
     */
    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    /**
     * Dimensione file in bytes
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // ==================== IMAGE DIMENSIONS ====================
    
    /**
     * Larghezza immagine in pixel
     */
    @Column(name = "width")
    private Integer width;

    /**
     * Altezza immagine in pixel
     */
    @Column(name = "height")
    private Integer height;

    // ==================== STORAGE PATHS ====================
    
    /**
     * Path relativo dell'immagine principale (ridimensionata/ottimizzata)
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * Path relativo della thumbnail
     */
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    /**
     * Path relativo dell'immagine originale (se mantenuta)
     */
    @Column(name = "original_path", length = 500)
    private String originalPath;

    // ==================== DISPLAY OPTIONS ====================
    
    /**
     * Titolo/descrizione dell'immagine
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * Testo alternativo per accessibilità
     */
    @Column(name = "alt_text", length = 255)
    private String altText;

    /**
     * Ordine di visualizzazione nella galleria
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Flag per immagine principale/featured
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Flag per immagine attiva/visibile
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ==================== AUDIT FIELDS ====================
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    // ==================== LIFECYCLE CALLBACKS ====================
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Verifica se è un logo
     */
    public boolean isLogo() {
        return imageType == ImageType.LOGO;
    }

    /**
     * Verifica se è un'immagine di copertina
     */
    public boolean isCover() {
        return imageType == ImageType.COVER;
    }

    /**
     * Verifica se è un'immagine della galleria
     */
    public boolean isGalleryImage() {
        return imageType == ImageType.GALLERY;
    }

    /**
     * Calcola l'aspect ratio dell'immagine
     */
    public Double getAspectRatio() {
        if (width != null && height != null && height > 0) {
            return (double) width / height;
        }
        return null;
    }

    /**
     * Restituisce una rappresentazione leggibile della dimensione file
     */
    public String getFileSizeReadable() {
        if (fileSize == null) return "N/A";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return "RestaurantGalleryImage{" +
                "id=" + id +
                ", restaurantId=" + (restaurant != null ? restaurant.getId() : null) +
                ", imageType=" + imageType +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + getFileSizeReadable() +
                ", dimensions=" + width + "x" + height +
                '}';
    }
}
