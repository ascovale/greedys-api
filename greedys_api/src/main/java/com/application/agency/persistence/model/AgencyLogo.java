package com.application.agency.persistence.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * ⭐ AGENCY LOGO ENTITY
 * 
 * Rappresenta il logo di un'agenzia.
 * Include metadati come dimensioni, thumbnail e info di compressione.
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "agency_logo", indexes = {
    @Index(name = "idx_agency_logo_agency", columnList = "agency_id"),
    @Index(name = "idx_agency_logo_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyLogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== RELATIONSHIP ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    @JsonBackReference
    private Agency agency;

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

    // ==================== DISPLAY OPTIONS ====================
    
    /**
     * Testo alternativo per accessibilità
     */
    @Column(name = "alt_text", length = 255)
    private String altText;

    /**
     * Flag per logo attivo/visibile
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
        return "AgencyLogo{" +
                "id=" + id +
                ", agencyId=" + (agency != null ? agency.getId() : null) +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + getFileSizeReadable() +
                ", dimensions=" + width + "x" + height +
                '}';
    }
}
