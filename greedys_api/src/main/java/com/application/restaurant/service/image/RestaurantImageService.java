package com.application.restaurant.service.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

import com.application.common.exception.ImageProcessingException;
import com.application.common.exception.InvalidImageException;
import com.application.common.persistence.dao.RestaurantGalleryImageDAO;
import com.application.common.persistence.model.RestaurantGalleryImage;
import com.application.common.persistence.model.RestaurantGalleryImage.ImageType;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.service.image.dto.ImageUploadResponse;
import com.application.restaurant.service.image.dto.RestaurantGalleryImageDTO;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RESTAURANT IMAGE SERVICE
 * 
 * Servizio per la gestione delle immagini del ristorante:
 * - Upload con validazione
 * - Ridimensionamento automatico
 * - Compressione JPEG
 * - Generazione thumbnail
 * - Gestione galleria, logo e cover
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantImageService {

    private final RestaurantGalleryImageDAO imageDAO;
    private final RestaurantDAO restaurantDAO;

    // ==================== CONFIGURATION ====================
    
    @Value("${app.image.upload-dir:uploads/restaurants}")
    private String uploadDir;

    @Value("${app.image.max-width:1920}")
    private int maxWidth;

    @Value("${app.image.max-height:1080}")
    private int maxHeight;

    @Value("${app.image.thumbnail-width:300}")
    private int thumbnailWidth;

    @Value("${app.image.thumbnail-height:200}")
    private int thumbnailHeight;

    @Value("${app.image.logo-width:400}")
    private int logoWidth;

    @Value("${app.image.logo-height:400}")
    private int logoHeight;

    @Value("${app.image.cover-width:1920}")
    private int coverWidth;

    @Value("${app.image.cover-height:600}")
    private int coverHeight;

    @Value("${app.image.compression-quality:0.85}")
    private float compressionQuality;

    @Value("${app.image.max-file-size:10485760}")  // 10MB default
    private long maxFileSize;

    @Value("${app.image.max-gallery-images:20}")
    private int maxGalleryImages;

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
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", uploadDir, e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    // ==================== UPLOAD OPERATIONS ====================
    
    /**
     * Upload un'immagine nella galleria del ristorante
     */
    @Transactional
    public ImageUploadResponse uploadGalleryImage(Long restaurantId, MultipartFile file, 
                                                   String title, String altText, Long userId) {
        validateFile(file);
        checkGalleryLimit(restaurantId);
        
        return processAndSaveImage(restaurantId, file, ImageType.GALLERY, title, altText, userId);
    }

    /**
     * Upload/sostituzione del logo del ristorante
     */
    @Transactional
    public ImageUploadResponse uploadLogo(Long restaurantId, MultipartFile file, Long userId) {
        validateFile(file);
        
        // Disattiva il logo precedente
        imageDAO.deactivateByRestaurantIdAndType(restaurantId, ImageType.LOGO);
        
        return processAndSaveImage(restaurantId, file, ImageType.LOGO, "Logo", "Logo del ristorante", userId);
    }

    /**
     * Upload/sostituzione dell'immagine di copertina
     */
    @Transactional
    public ImageUploadResponse uploadCover(Long restaurantId, MultipartFile file, 
                                            String title, String altText, Long userId) {
        validateFile(file);
        
        // Disattiva la cover precedente
        imageDAO.deactivateByRestaurantIdAndType(restaurantId, ImageType.COVER);
        
        return processAndSaveImage(restaurantId, file, ImageType.COVER, title, altText, userId);
    }

    // ==================== RETRIEVAL OPERATIONS ====================
    
    /**
     * Ottiene tutte le immagini della galleria di un ristorante
     */
    @Transactional(readOnly = true)
    public List<RestaurantGalleryImageDTO> getGalleryImages(Long restaurantId) {
        return imageDAO.findGalleryImagesByRestaurantId(restaurantId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Ottiene il logo del ristorante
     */
    @Transactional(readOnly = true)
    public RestaurantGalleryImageDTO getLogo(Long restaurantId) {
        return imageDAO.findLogoByRestaurantId(restaurantId)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Ottiene l'immagine di copertina del ristorante
     */
    @Transactional(readOnly = true)
    public RestaurantGalleryImageDTO getCover(Long restaurantId) {
        return imageDAO.findCoverByRestaurantId(restaurantId)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Ottiene tutte le immagini del ristorante (logo, cover, galleria)
     */
    @Transactional(readOnly = true)
    public List<RestaurantGalleryImageDTO> getAllImages(Long restaurantId) {
        return imageDAO.findByRestaurantIdAndIsActiveTrueOrderByDisplayOrderAsc(restaurantId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Ottiene un'immagine per ID
     */
    @Transactional(readOnly = true)
    public RestaurantGalleryImageDTO getImageById(Long imageId) {
        return imageDAO.findById(imageId)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
    }

    // ==================== UPDATE OPERATIONS ====================
    
    /**
     * Aggiorna i metadati di un'immagine
     */
    @Transactional
    public RestaurantGalleryImageDTO updateImageMetadata(Long imageId, String title, 
                                                          String altText, Integer displayOrder) {
        RestaurantGalleryImage image = imageDAO.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
        
        if (title != null) {
            image.setTitle(title);
        }
        if (altText != null) {
            image.setAltText(altText);
        }
        if (displayOrder != null) {
            image.setDisplayOrder(displayOrder);
        }
        
        return toDTO(imageDAO.save(image));
    }

    /**
     * Imposta un'immagine come featured
     */
    @Transactional
    public RestaurantGalleryImageDTO setFeatured(Long restaurantId, Long imageId) {
        RestaurantGalleryImage image = imageDAO.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
        
        if (!image.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Image does not belong to restaurant");
        }
        
        // Reset altri featured
        imageDAO.resetFeaturedForRestaurant(restaurantId);
        
        image.setIsFeatured(true);
        return toDTO(imageDAO.save(image));
    }

    /**
     * Riordina le immagini della galleria
     */
    @Transactional
    public void reorderImages(Long restaurantId, List<Long> imageIds) {
        for (int i = 0; i < imageIds.size(); i++) {
            imageDAO.updateDisplayOrder(imageIds.get(i), i + 1);
        }
    }

    // ==================== DELETE OPERATIONS ====================
    
    /**
     * Elimina un'immagine (soft delete)
     */
    @Transactional
    public void deleteImage(Long restaurantId, Long imageId) {
        RestaurantGalleryImage image = imageDAO.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
        
        if (!image.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Image does not belong to restaurant");
        }
        
        imageDAO.softDeleteById(imageId);
        log.info("Soft deleted image {} for restaurant {}", imageId, restaurantId);
    }

    /**
     * Elimina definitivamente un'immagine e i file associati
     */
    @Transactional
    public void hardDeleteImage(Long restaurantId, Long imageId) {
        RestaurantGalleryImage image = imageDAO.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
        
        if (!image.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Image does not belong to restaurant");
        }
        
        // Elimina i file fisici
        deletePhysicalFiles(image);
        
        // Elimina il record dal DB
        imageDAO.delete(image);
        log.info("Hard deleted image {} for restaurant {}", imageId, restaurantId);
    }

    // ==================== FILE PATH RESOLUTION ====================
    
    /**
     * Restituisce il path assoluto di un'immagine
     */
    public Path resolveImagePath(String relativePath) {
        return Paths.get(uploadDir).resolve(relativePath);
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

    private void checkGalleryLimit(Long restaurantId) {
        long currentCount = imageDAO.countGalleryImagesByRestaurantId(restaurantId);
        if (currentCount >= maxGalleryImages) {
            throw new InvalidImageException(
                String.format("Raggiunto il limite massimo di %d immagini nella galleria", maxGalleryImages));
        }
    }

    private ImageUploadResponse processAndSaveImage(Long restaurantId, MultipartFile file, 
                                                     ImageType imageType, String title, 
                                                     String altText, Long userId) {
        try {
            Restaurant restaurant = restaurantDAO.findById(restaurantId)
                    .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));

            // Leggi l'immagine originale
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new InvalidImageException("Impossibile leggere l'immagine. Il file potrebbe essere corrotto.");
            }

            // Determina le dimensioni target in base al tipo
            int targetWidth, targetHeight;
            switch (imageType) {
                case LOGO:
                    targetWidth = logoWidth;
                    targetHeight = logoHeight;
                    break;
                case COVER:
                    targetWidth = coverWidth;
                    targetHeight = coverHeight;
                    break;
                default:
                    targetWidth = maxWidth;
                    targetHeight = maxHeight;
            }

            // Genera nomi file
            String uuid = UUID.randomUUID().toString();
            String extension = getFileExtension(file.getOriginalFilename());
            String baseFilename = uuid + extension;
            String thumbnailFilename = uuid + "_thumb" + extension;

            // Crea directory per ristorante
            Path restaurantDir = Paths.get(uploadDir, String.valueOf(restaurantId));
            Files.createDirectories(restaurantDir);

            // Ridimensiona e salva l'immagine principale
            BufferedImage resizedImage = resizeImage(originalImage, targetWidth, targetHeight);
            byte[] compressedBytes = compressImage(resizedImage, extension);
            Path mainPath = restaurantDir.resolve(baseFilename);
            Files.write(mainPath, compressedBytes);

            // Genera e salva thumbnail
            BufferedImage thumbnail = resizeImage(originalImage, thumbnailWidth, thumbnailHeight);
            byte[] thumbnailBytes = compressImage(thumbnail, extension);
            Path thumbPath = restaurantDir.resolve(thumbnailFilename);
            Files.write(thumbPath, thumbnailBytes);

            // Salva l'originale se richiesto (per immagini grandi)
            String originalRelativePath = null;
            if (originalImage.getWidth() > targetWidth || originalImage.getHeight() > targetHeight) {
                String originalFilename = uuid + "_original" + extension;
                Path originalPath = restaurantDir.resolve(originalFilename);
                Files.copy(file.getInputStream(), originalPath, StandardCopyOption.REPLACE_EXISTING);
                originalRelativePath = restaurantId + "/" + originalFilename;
            }

            // Crea e salva l'entità
            RestaurantGalleryImage entity = RestaurantGalleryImage.builder()
                    .restaurant(restaurant)
                    .imageType(imageType)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(baseFilename)
                    .mimeType(file.getContentType())
                    .fileSize((long) compressedBytes.length)
                    .width(resizedImage.getWidth())
                    .height(resizedImage.getHeight())
                    .filePath(restaurantId + "/" + baseFilename)
                    .thumbnailPath(restaurantId + "/" + thumbnailFilename)
                    .originalPath(originalRelativePath)
                    .title(title)
                    .altText(altText)
                    .displayOrder(imageType == ImageType.GALLERY ? 
                            imageDAO.findNextDisplayOrder(restaurantId) : 0)
                    .isFeatured(false)
                    .isActive(true)
                    .createdBy(userId)
                    .build();

            RestaurantGalleryImage saved = imageDAO.save(entity);
            log.info("Uploaded {} image {} for restaurant {}", imageType, saved.getId(), restaurantId);

            return ImageUploadResponse.builder()
                    .id(saved.getId())
                    .imageType(imageType.name())
                    .filePath(saved.getFilePath())
                    .thumbnailPath(saved.getThumbnailPath())
                    .width(saved.getWidth())
                    .height(saved.getHeight())
                    .fileSize(saved.getFileSize())
                    .fileSizeReadable(saved.getFileSizeReadable())
                    .message("Immagine caricata con successo")
                    .build();

        } catch (IOException e) {
            log.error("Error processing image for restaurant {}: {}", restaurantId, e.getMessage(), e);
            throw new ImageProcessingException("Errore durante l'elaborazione dell'immagine", e);
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

    private void deletePhysicalFiles(RestaurantGalleryImage image) {
        try {
            if (image.getFilePath() != null) {
                Files.deleteIfExists(resolveImagePath(image.getFilePath()));
            }
            if (image.getThumbnailPath() != null) {
                Files.deleteIfExists(resolveImagePath(image.getThumbnailPath()));
            }
            if (image.getOriginalPath() != null) {
                Files.deleteIfExists(resolveImagePath(image.getOriginalPath()));
            }
        } catch (IOException e) {
            log.warn("Could not delete physical files for image {}: {}", image.getId(), e.getMessage());
        }
    }

    private RestaurantGalleryImageDTO toDTO(RestaurantGalleryImage entity) {
        return RestaurantGalleryImageDTO.builder()
                .id(entity.getId())
                .restaurantId(entity.getRestaurant().getId())
                .imageType(entity.getImageType().name())
                .originalFilename(entity.getOriginalFilename())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .fileSizeReadable(entity.getFileSizeReadable())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .aspectRatio(entity.getAspectRatio())
                .filePath(entity.getFilePath())
                .thumbnailPath(entity.getThumbnailPath())
                .title(entity.getTitle())
                .altText(entity.getAltText())
                .displayOrder(entity.getDisplayOrder())
                .isFeatured(entity.getIsFeatured())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
