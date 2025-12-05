package com.application.restaurant.controller.restaurant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.service.image.RestaurantImageService;
import com.application.restaurant.service.image.dto.ImageUploadResponse;
import com.application.restaurant.service.image.dto.RestaurantGalleryImageDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⭐ RESTAURANT IMAGE CONTROLLER
 * 
 * Controller per la gestione delle immagini del ristorante:
 * - Upload logo (singolo, sostituisce il precedente)
 * - Upload immagine di copertina (singola, sostituisce la precedente)
 * - Gestione galleria immagini (multiple)
 * - Ridimensionamento automatico e compressione
 * - Generazione thumbnail
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Tag(name = "Restaurant Image Gallery", description = "Gestione galleria immagini, logo e copertina del ristorante")
@RestController
@RequestMapping("/restaurant/images")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class RestaurantImageController extends BaseController {

    private final RestaurantImageService imageService;

    // ==================== LOGO OPERATIONS ====================

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload logo del ristorante",
        description = """
            Carica il logo del ristorante. Se esiste già un logo, viene sostituito.
            
            **Specifiche logo:**
            - Dimensione massima: 400x400 pixel (ridimensionato automaticamente)
            - Formati supportati: JPEG, PNG, WebP, GIF
            - Dimensione massima file: 10MB
            - Compressione JPEG automatica (qualità 85%)
            """
    )
    @ApiResponse(responseCode = "201", description = "Logo caricato con successo")
    @ApiResponse(responseCode = "400", description = "File non valido o formato non supportato")
    @CreateApiResponses
    public ResponseEntity<ImageUploadResponse> uploadLogo(
            @Parameter(description = "File immagine del logo", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeCreate("upload logo", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            Long userId = rUser.getId();
            log.info("Uploading logo for restaurant ID: {} by user ID: {}", restaurantId, userId);
            return imageService.uploadLogo(restaurantId, file, userId);
        });
    }

    @GetMapping("/logo")
    @Operation(summary = "Ottieni informazioni sul logo", description = "Restituisce i metadati del logo del ristorante")
    @ReadApiResponses
    public ResponseEntity<RestaurantGalleryImageDTO> getLogo(@AuthenticationPrincipal RUser rUser) {
        return execute("get logo", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return imageService.getLogo(restaurantId);
        });
    }

    // ==================== COVER IMAGE OPERATIONS ====================

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload immagine di copertina",
        description = """
            Carica l'immagine di copertina del ristorante. Se esiste già, viene sostituita.
            
            **Specifiche copertina:**
            - Dimensione massima: 1920x600 pixel (ridimensionato automaticamente)
            - Aspect ratio consigliato: 3.2:1 (panoramico)
            - Formati supportati: JPEG, PNG, WebP, GIF
            - Dimensione massima file: 10MB
            """
    )
    @ApiResponse(responseCode = "201", description = "Copertina caricata con successo")
    @CreateApiResponses
    public ResponseEntity<ImageUploadResponse> uploadCover(
            @Parameter(description = "File immagine della copertina", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Titolo/descrizione dell'immagine")
            @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "Testo alternativo per accessibilità")
            @RequestParam(value = "altText", required = false) String altText,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeCreate("upload cover", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            Long userId = rUser.getId();
            log.info("Uploading cover for restaurant ID: {} by user ID: {}", restaurantId, userId);
            return imageService.uploadCover(restaurantId, file, title, altText, userId);
        });
    }

    @GetMapping("/cover")
    @Operation(summary = "Ottieni informazioni sulla copertina", description = "Restituisce i metadati dell'immagine di copertina")
    @ReadApiResponses
    public ResponseEntity<RestaurantGalleryImageDTO> getCover(@AuthenticationPrincipal RUser rUser) {
        return execute("get cover", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return imageService.getCover(restaurantId);
        });
    }

    // ==================== GALLERY OPERATIONS ====================

    @PostMapping(value = "/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload immagine nella galleria",
        description = """
            Aggiunge un'immagine alla galleria del ristorante.
            
            **Specifiche galleria:**
            - Dimensione massima: 1920x1080 pixel (ridimensionato automaticamente)
            - Massimo 20 immagini per ristorante
            - Formati supportati: JPEG, PNG, WebP, GIF
            - Dimensione massima file: 10MB
            - Thumbnail generata automaticamente (300x200 pixel)
            """
    )
    @ApiResponse(responseCode = "201", description = "Immagine aggiunta alla galleria")
    @CreateApiResponses
    public ResponseEntity<ImageUploadResponse> uploadGalleryImage(
            @Parameter(description = "File immagine", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Titolo/descrizione dell'immagine")
            @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "Testo alternativo per accessibilità")
            @RequestParam(value = "altText", required = false) String altText,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeCreate("upload gallery image", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            Long userId = rUser.getId();
            log.info("Uploading gallery image for restaurant ID: {} by user ID: {}", restaurantId, userId);
            return imageService.uploadGalleryImage(restaurantId, file, title, altText, userId);
        });
    }

    @GetMapping("/gallery")
    @Operation(summary = "Ottieni galleria immagini", description = "Restituisce tutte le immagini della galleria del ristorante")
    @ReadApiResponses
    public ResponseEntity<List<RestaurantGalleryImageDTO>> getGalleryImages(@AuthenticationPrincipal RUser rUser) {
        return executeList("get gallery images", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return imageService.getGalleryImages(restaurantId);
        });
    }

    @GetMapping
    @Operation(summary = "Ottieni tutte le immagini", description = "Restituisce logo, copertina e galleria del ristorante")
    @ReadApiResponses
    public ResponseEntity<List<RestaurantGalleryImageDTO>> getAllImages(@AuthenticationPrincipal RUser rUser) {
        return executeList("get all images", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            return imageService.getAllImages(restaurantId);
        });
    }

    // ==================== UPDATE OPERATIONS ====================

    @PutMapping("/{imageId}")
    @Operation(summary = "Aggiorna metadati immagine", description = "Aggiorna titolo, testo alternativo e ordine di un'immagine")
    @ReadApiResponses
    public ResponseEntity<RestaurantGalleryImageDTO> updateImageMetadata(
            @PathVariable Long imageId,
            @Parameter(description = "Nuovo titolo")
            @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "Nuovo testo alternativo")
            @RequestParam(value = "altText", required = false) String altText,
            @Parameter(description = "Nuovo ordine di visualizzazione")
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @AuthenticationPrincipal RUser rUser) {
        
        return execute("update image metadata", () -> {
            log.info("Updating metadata for image ID: {}", imageId);
            return imageService.updateImageMetadata(imageId, title, altText, displayOrder);
        });
    }

    @PutMapping("/{imageId}/featured")
    @Operation(summary = "Imposta immagine in evidenza", description = "Imposta un'immagine come featured/principale")
    @ReadApiResponses
    public ResponseEntity<RestaurantGalleryImageDTO> setFeatured(
            @PathVariable Long imageId,
            @AuthenticationPrincipal RUser rUser) {
        
        return execute("set featured image", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            log.info("Setting image {} as featured for restaurant {}", imageId, restaurantId);
            return imageService.setFeatured(restaurantId, imageId);
        });
    }

    @PutMapping("/gallery/reorder")
    @Operation(
        summary = "Riordina galleria",
        description = "Riordina le immagini della galleria in base all'ordine degli ID forniti"
    )
    public ResponseEntity<Void> reorderGallery(
            @Parameter(description = "Lista degli ID delle immagini nell'ordine desiderato")
            @RequestParam List<Long> imageIds,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeVoid("reorder gallery", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            log.info("Reordering gallery for restaurant {}: {}", restaurantId, imageIds);
            imageService.reorderImages(restaurantId, imageIds);
        });
    }

    // ==================== DELETE OPERATIONS ====================

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Elimina immagine", description = "Elimina un'immagine dalla galleria (soft delete)")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long imageId,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeVoid("delete image", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            log.info("Deleting image {} for restaurant {}", imageId, restaurantId);
            imageService.deleteImage(restaurantId, imageId);
        });
    }

    @DeleteMapping("/{imageId}/permanent")
    @Operation(
        summary = "Elimina immagine definitivamente",
        description = "Elimina permanentemente un'immagine e i file associati dal server"
    )
    public ResponseEntity<Void> hardDeleteImage(
            @PathVariable Long imageId,
            @AuthenticationPrincipal RUser rUser) {
        
        return executeVoid("hard delete image", () -> {
            Long restaurantId = rUser.getRestaurant().getId();
            log.info("Hard deleting image {} for restaurant {}", imageId, restaurantId);
            imageService.hardDeleteImage(restaurantId, imageId);
        });
    }

    // ==================== FILE SERVING ====================

    @GetMapping("/file/{restaurantId}/{filename:.+}")
    @Operation(
        summary = "Scarica file immagine",
        description = "Endpoint pubblico per scaricare un'immagine. Restituisce il file binario."
    )
    @ApiResponse(
        responseCode = "200",
        description = "File immagine",
        content = @Content(mediaType = "image/*")
    )
    @ApiResponse(responseCode = "404", description = "File non trovato")
    public ResponseEntity<Resource> serveFile(
            @PathVariable Long restaurantId,
            @PathVariable String filename,
            HttpServletRequest request) throws MalformedURLException {
        
        String relativePath = restaurantId + "/" + filename;
        Path filePath = imageService.resolveImagePath(relativePath);
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("File not found or not readable: {}", relativePath);
            return ResponseEntity.notFound().build();
        }
        
        // Determina il content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.debug("Could not determine file type for: {}", filename);
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // Cache 24h
                .body(resource);
    }

    // ==================== PUBLIC ENDPOINTS (No Auth) ====================

    @GetMapping("/public/{restaurantId}/gallery")
    @Operation(
        summary = "Galleria pubblica",
        description = "Endpoint pubblico per ottenere la galleria di un ristorante (senza autenticazione)"
    )
    @ReadApiResponses
    public ResponseEntity<List<RestaurantGalleryImageDTO>> getPublicGallery(
            @PathVariable Long restaurantId) {
        return executeList("get public gallery", () -> {
            log.debug("Getting public gallery for restaurant {}", restaurantId);
            return imageService.getGalleryImages(restaurantId);
        });
    }

    @GetMapping("/public/{restaurantId}/logo")
    @Operation(summary = "Logo pubblico", description = "Endpoint pubblico per ottenere il logo di un ristorante")
    @ReadApiResponses
    public ResponseEntity<RestaurantGalleryImageDTO> getPublicLogo(
            @PathVariable Long restaurantId) {
        return execute("get public logo", () -> {
            return imageService.getLogo(restaurantId);
        });
    }

    @GetMapping("/public/{restaurantId}/cover")
    @Operation(summary = "Copertina pubblica", description = "Endpoint pubblico per ottenere la copertina di un ristorante")
    @ReadApiResponses
    public ResponseEntity<RestaurantGalleryImageDTO> getPublicCover(
            @PathVariable Long restaurantId) {
        return execute("get public cover", () -> {
            return imageService.getCover(restaurantId);
        });
    }

    @GetMapping("/public/{restaurantId}")
    @Operation(summary = "Tutte le immagini pubbliche", description = "Endpoint pubblico per ottenere tutte le immagini di un ristorante")
    @ReadApiResponses
    public ResponseEntity<List<RestaurantGalleryImageDTO>> getAllPublicImages(
            @PathVariable Long restaurantId) {
        return executeList("get all public images", () -> {
            return imageService.getAllImages(restaurantId);
        });
    }
}
