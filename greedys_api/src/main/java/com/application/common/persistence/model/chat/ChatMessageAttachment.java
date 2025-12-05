package com.application.common.persistence.model.chat;

import java.time.Instant;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ⭐ CHAT MESSAGE ATTACHMENT ENTITY
 * 
 * Rappresenta un file allegato a un messaggio chat.
 * 
 * FEATURES:
 * - Supporto per immagini, documenti, video
 * - Storage URL (S3/MinIO)
 * - Thumbnail per preview
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Entity
@Table(name = "chat_message_attachment", indexes = {
    @Index(name = "idx_attachment_message", columnList = "message_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Messaggio di appartenenza
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    /**
     * Nome originale del file
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * MIME type del file
     */
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    /**
     * Dimensione in bytes
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * URL di storage (S3/MinIO)
     */
    @Column(name = "storage_url", nullable = false, length = 500)
    private String storageUrl;

    /**
     * URL thumbnail (per immagini/video)
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * Data upload
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Verifica se è un'immagine
     */
    public boolean isImage() {
        return fileType != null && fileType.startsWith("image/");
    }

    /**
     * Verifica se è un video
     */
    public boolean isVideo() {
        return fileType != null && fileType.startsWith("video/");
    }

    /**
     * Verifica se è un documento
     */
    public boolean isDocument() {
        return !isImage() && !isVideo();
    }

    /**
     * Formatta la dimensione in modo leggibile
     */
    public String getFormattedSize() {
        if (fileSize == null) return "0 B";
        
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }
}
