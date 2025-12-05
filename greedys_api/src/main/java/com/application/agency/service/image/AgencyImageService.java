package com.application.agency.service.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.application.agency.persistence.dao.AgencyDAO;
import com.application.agency.persistence.dao.AgencyLogoDAO;
import com.application.agency.persistence.model.Agency;
import com.application.agency.persistence.model.AgencyLogo;
import com.application.agency.service.image.dto.AgencyLogoDTO;
import com.application.agency.service.image.dto.AgencyLogoUploadResponse;
import com.application.common.exception.ImageProcessingException;
import com.application.common.exception.InvalidImageException;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ AGENCY IMAGE SERVICE
 * 
 * Servizio per la gestione del logo dell'agenzia:
 * - Upload con validazione
 * - Ridimensionamento automatico
 * - Compressione JPEG
 * - Generazione thumbnail
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyImageService {

    private final AgencyLogoDAO logoDAO;
    private final AgencyDAO agencyDAO;

    // ==================== CONFIGURATION ====================
    
    @Value("${app.image.upload-dir:uploads/agencies}")
    private String uploadDir;

    @Value("${app.image.agency-logo-width:400}")
    private int logoWidth;

    @Value("${app.image.agency-logo-height:400}")
    private int logoHeight;

    @Value("${app.image.thumbnail-width:150}")
    private int thumbnailWidth;

    @Value("${app.image.thumbnail-height:150}")
    private int thumbnailHeight;

    @Value("${app.image.compression-quality:0.85}")
    private float compressionQuality;

    @Value("${app.image.max-file-size:10485760}")  // 10MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    // ==================== INITIALIZATION ====================
    
    @PostConstruct
    public void init() {
        try {
            Path agenciesDir = Paths.get(uploadDir.replace("restaurants", "agencies"));
            if (!Files.exists(agenciesDir)) {
                Files.createDirectories(agenciesDir);
                log.info("Created agency upload directory: {}", agenciesDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not create agency upload directory", e);
            throw new RuntimeException("Could not initialize agency storage", e);
        }
    }

    // ==================== UPLOAD OPERATIONS ====================
    
    /**
     * Upload/sostituzione del logo dell'agenzia
     */
    @Transactional
    public AgencyLogoUploadResponse uploadLogo(Long agencyId, MultipartFile file, 
                                                String altText, Long userId) {
        validateFile(file);
        
        // Disattiva il logo precedente
        logoDAO.deactivateAllByAgencyId(agencyId);
        
        return processAndSaveLogo(agencyId, file, altText, userId);
    }

    // ==================== RETRIEVAL OPERATIONS ====================
    
    /**
     * Ottiene il logo attivo dell'agenzia
     */
    @Transactional(readOnly = true)
    public AgencyLogoDTO getLogo(Long agencyId) {
        return logoDAO.findActiveLogoByAgencyId(agencyId)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Verifica se l'agenzia ha un logo
     */
    @Transactional(readOnly = true)
    public boolean hasLogo(Long agencyId) {
        return logoDAO.existsByAgencyIdAndIsActiveTrue(agencyId);
    }

    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Elimina il logo dell'agenzia (soft delete)
     */
    @Transactional
    public void deleteLogo(Long agencyId) {
        logoDAO.deactivateAllByAgencyId(agencyId);
        log.info("Soft deleted logo for agency {}", agencyId);
    }

    /**
     * Elimina definitivamente il logo e i file associati
     */
    @Transactional
    public void hardDeleteLogo(Long agencyId) {
        AgencyLogo logo = logoDAO.findActiveLogoByAgencyId(agencyId)
                .orElseThrow(() -> new EntityNotFoundException("Logo not found for agency: " + agencyId));
        
        // Elimina i file fisici
        deletePhysicalFiles(logo);
        
        // Elimina il record dal DB
        logoDAO.delete(logo);
        log.info("Hard deleted logo for agency {}", agencyId);
    }

    // ==================== FILE PATH RESOLUTION ====================
    
    /**
     * Restituisce il path assoluto di un'immagine
     */
    public Path resolveImagePath(String relativePath) {
        String basePath = uploadDir.replace("restaurants", "agencies");
        return Paths.get(basePath).resolve(relativePath);
    }

    /**
     * Verifica se un file esiste
     */
    public boolean imageExists(String relativePath) {
        return Files.exists(resolveImagePath(relativePath));
    }

    // ==================== PRIVATE METHODS ====================
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Il file è vuoto o non fornito");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new InvalidImageException(
                String.format("Il file supera la dimensione massima di %d MB", maxFileSize / (1024 * 1024)));
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageException(
                "Tipo di file non supportato. Tipi ammessi: " + String.join(", ", ALLOWED_MIME_TYPES));
        }
        
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = getFileExtension(filename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new InvalidImageException(
                    "Estensione file non supportata. Estensioni ammesse: " + String.join(", ", ALLOWED_EXTENSIONS));
            }
        }
    }

    private AgencyLogoUploadResponse processAndSaveLogo(Long agencyId, MultipartFile file, 
                                                         String altText, Long userId) {
        try {
            Agency agency = agencyDAO.findById(agencyId)
                    .orElseThrow(() -> new EntityNotFoundException("Agency not found: " + agencyId));

            // Leggi l'immagine originale
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new InvalidImageException("Impossibile leggere l'immagine. Il file potrebbe essere corrotto.");
            }

            // Genera nomi file
            String uuid = UUID.randomUUID().toString();
            String extension = getFileExtension(file.getOriginalFilename());
            String baseFilename = uuid + extension;
            String thumbnailFilename = uuid + "_thumb" + extension;

            // Crea directory per agenzia
            String basePath = uploadDir.replace("restaurants", "agencies");
            Path agencyDir = Paths.get(basePath, String.valueOf(agencyId));
            Files.createDirectories(agencyDir);

            // Ridimensiona e salva il logo principale
            BufferedImage resizedImage = resizeImage(originalImage, logoWidth, logoHeight);
            byte[] compressedBytes = compressImage(resizedImage, extension);
            Path mainPath = agencyDir.resolve(baseFilename);
            Files.write(mainPath, compressedBytes);

            // Genera e salva thumbnail
            BufferedImage thumbnail = resizeImage(originalImage, thumbnailWidth, thumbnailHeight);
            byte[] thumbnailBytes = compressImage(thumbnail, extension);
            Path thumbPath = agencyDir.resolve(thumbnailFilename);
            Files.write(thumbPath, thumbnailBytes);

            // Crea e salva l'entità
            AgencyLogo entity = AgencyLogo.builder()
                    .agency(agency)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(baseFilename)
                    .mimeType(file.getContentType())
                    .fileSize((long) compressedBytes.length)
                    .width(resizedImage.getWidth())
                    .height(resizedImage.getHeight())
                    .filePath(agencyId + "/" + baseFilename)
                    .thumbnailPath(agencyId + "/" + thumbnailFilename)
                    .altText(altText != null ? altText : "Logo " + agency.getName())
                    .isActive(true)
                    .createdBy(userId)
                    .build();

            AgencyLogo saved = logoDAO.save(entity);
            log.info("Uploaded logo {} for agency {}", saved.getId(), agencyId);

            return AgencyLogoUploadResponse.builder()
                    .id(saved.getId())
                    .filePath(saved.getFilePath())
                    .thumbnailPath(saved.getThumbnailPath())
                    .width(saved.getWidth())
                    .height(saved.getHeight())
                    .fileSize(saved.getFileSize())
                    .fileSizeReadable(saved.getFileSizeReadable())
                    .message("Logo caricato con successo")
                    .build();

        } catch (IOException e) {
            log.error("Error processing logo for agency {}: {}", agencyId, e.getMessage(), e);
            throw new ImageProcessingException("Errore durante l'elaborazione del logo", e);
        }
    }

    private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // Se l'immagine è già più piccola, non ridimensionare
        if (originalWidth <= targetWidth && originalHeight <= targetHeight) {
            return original;
        }

        // Calcola le nuove dimensioni mantenendo l'aspect ratio
        double widthRatio = (double) targetWidth / originalWidth;
        double heightRatio = (double) targetHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Crea l'immagine ridimensionata con alta qualità
        BufferedImage resized = new BufferedImage(newWidth, newHeight, 
                original.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : original.getType());
        
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resized;
    }

    private byte[] compressImage(BufferedImage image, String extension) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = getFormatName(extension);

        // Per JPEG, applica compressione con qualità configurabile
        if (formatName.equalsIgnoreCase("jpeg") || formatName.equalsIgnoreCase("jpg")) {
            // Converti in RGB se ha canale alpha (JPEG non supporta trasparenza)
            BufferedImage rgbImage = image;
            if (image.getType() == BufferedImage.TYPE_INT_ARGB || 
                image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
                rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgbImage.createGraphics();
                g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
                g.dispose();
            }

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(compressionQuality);

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgbImage, null, null), param);
            }
            writer.dispose();
        } else {
            // Per altri formati (PNG, WebP, GIF), usa compressione standard
            ImageIO.write(image, formatName, baos);
        }

        return baos.toByteArray();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // default
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private String getFormatName(String extension) {
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "jpeg";
            case ".png" -> "png";
            case ".webp" -> "webp";
            case ".gif" -> "gif";
            default -> "jpeg";
        };
    }

    private void deletePhysicalFiles(AgencyLogo logo) {
        try {
            if (logo.getFilePath() != null) {
                Files.deleteIfExists(resolveImagePath(logo.getFilePath()));
            }
            if (logo.getThumbnailPath() != null) {
                Files.deleteIfExists(resolveImagePath(logo.getThumbnailPath()));
            }
        } catch (IOException e) {
            log.warn("Could not delete physical files for logo {}: {}", logo.getId(), e.getMessage());
        }
    }

    private AgencyLogoDTO toDTO(AgencyLogo entity) {
        return AgencyLogoDTO.builder()
                .id(entity.getId())
                .agencyId(entity.getAgency().getId())
                .originalFilename(entity.getOriginalFilename())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .fileSizeReadable(entity.getFileSizeReadable())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .aspectRatio(entity.getAspectRatio())
                .filePath(entity.getFilePath())
                .thumbnailPath(entity.getThumbnailPath())
                .altText(entity.getAltText())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
