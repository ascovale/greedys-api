package com.application.agency.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

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

import com.application.agency.persistence.model.user.AgencyUserHub;
import com.application.agency.service.AgencyService;
import com.application.agency.service.image.AgencyImageService;
import com.application.agency.service.image.dto.AgencyLogoDTO;
import com.application.agency.service.image.dto.AgencyLogoUploadResponse;
import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;

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
 * ⭐ AGENCY IMAGE CONTROLLER
 * 
 * Controller per la gestione delle immagini dell'agenzia:
 * - Upload logo (singolo, sostituisce il precedente)
 * - Ridimensionamento automatico e compressione
 * - Generazione thumbnail
 * 
 * @author Greedy's System
 * @since 2025-12-01
 */
@Tag(name = "Agency Logo Management", description = "Gestione logo e immagine profilo dell'agenzia")
@RestController
@RequestMapping("/agency/images")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class AgencyImageController extends BaseController {

    private final AgencyImageService imageService;
    private final AgencyService agencyService;

    // ==================== LOGO OPERATIONS ====================

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload logo dell'agenzia",
        description = """
            Carica il logo dell'agenzia. Se esiste già un logo, viene sostituito.
            
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
    public ResponseEntity<AgencyLogoUploadResponse> uploadLogo(
            @Parameter(description = "File immagine del logo", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID dell'agenzia", required = true)
            @RequestParam("agencyId") Long agencyId,
            @Parameter(description = "Testo alternativo per accessibilità")
            @RequestParam(value = "altText", required = false) String altText,
            @AuthenticationPrincipal AgencyUserHub userHub) {
        
        return executeCreate("upload agency logo", () -> {
            // Verifica che l'utente abbia accesso a questa agenzia
            validateAgencyAccess(userHub, agencyId);
            
            Long userId = userHub.getId();
            log.info("Uploading logo for agency ID: {} by user ID: {}", agencyId, userId);
            return imageService.uploadLogo(agencyId, file, altText, userId);
        });
    }

    @GetMapping("/logo/{agencyId}")
    @Operation(summary = "Ottieni informazioni sul logo", description = "Restituisce i metadati del logo dell'agenzia")
    @ReadApiResponses
    public ResponseEntity<AgencyLogoDTO> getLogo(
            @PathVariable Long agencyId,
            @AuthenticationPrincipal AgencyUserHub userHub) {
        return execute("get agency logo", () -> {
            validateAgencyAccess(userHub, agencyId);
            return imageService.getLogo(agencyId);
        });
    }

    @DeleteMapping("/logo/{agencyId}")
    @Operation(summary = "Elimina logo", description = "Elimina il logo dell'agenzia (soft delete)")
    public ResponseEntity<Void> deleteLogo(
            @PathVariable Long agencyId,
            @AuthenticationPrincipal AgencyUserHub userHub) {
        
        return executeVoid("delete agency logo", () -> {
            validateAgencyAccess(userHub, agencyId);
            log.info("Deleting logo for agency {}", agencyId);
            imageService.deleteLogo(agencyId);
        });
    }

    @DeleteMapping("/logo/{agencyId}/permanent")
    @Operation(
        summary = "Elimina logo definitivamente",
        description = "Elimina permanentemente il logo e i file associati dal server"
    )
    public ResponseEntity<Void> hardDeleteLogo(
            @PathVariable Long agencyId,
            @AuthenticationPrincipal AgencyUserHub userHub) {
        
        return executeVoid("hard delete agency logo", () -> {
            validateAgencyAccess(userHub, agencyId);
            log.info("Hard deleting logo for agency {}", agencyId);
            imageService.hardDeleteLogo(agencyId);
        });
    }

    // ==================== DESCRIPTION OPERATIONS ====================

    @PutMapping("/description/{agencyId}")
    @Operation(
        summary = "Aggiorna descrizione agenzia",
        description = "Aggiorna la descrizione testuale dell'agenzia (max 500 caratteri)"
    )
    @ReadApiResponses
    public ResponseEntity<String> updateDescription(
            @PathVariable Long agencyId,
            @Parameter(description = "Nuova descrizione", required = true)
            @RequestParam("description") String description,
            @AuthenticationPrincipal AgencyUserHub userHub) {
        
        return execute("update agency description", () -> {
            validateAgencyAccess(userHub, agencyId);
            
            if (description != null && description.length() > 500) {
                throw new IllegalArgumentException("La descrizione non può superare i 500 caratteri");
            }
            
            log.info("Updating description for agency {}", agencyId);
            agencyService.updateDescription(agencyId, description);
            return "Descrizione aggiornata con successo";
        });
    }

    @GetMapping("/description/{agencyId}")
    @Operation(summary = "Ottieni descrizione agenzia", description = "Restituisce la descrizione dell'agenzia")
    @ReadApiResponses
    public ResponseEntity<String> getDescription(
            @PathVariable Long agencyId,
            @AuthenticationPrincipal AgencyUserHub userHub) {
        return execute("get agency description", () -> {
            validateAgencyAccess(userHub, agencyId);
            return agencyService.getDescription(agencyId);
        });
    }

    // ==================== FILE SERVING ====================

    @GetMapping("/file/{agencyId}/{filename:.+}")
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
            @PathVariable Long agencyId,
            @PathVariable String filename,
            HttpServletRequest request) throws MalformedURLException {
        
        String relativePath = agencyId + "/" + filename;
        Path filePath = imageService.resolveImagePath(relativePath);
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("Agency file not found or not readable: {}", relativePath);
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

    @GetMapping("/public/{agencyId}/logo")
    @Operation(summary = "Logo pubblico", description = "Endpoint pubblico per ottenere il logo di un'agenzia")
    @ReadApiResponses
    public ResponseEntity<AgencyLogoDTO> getPublicLogo(@PathVariable Long agencyId) {
        return execute("get public agency logo", () -> {
            return imageService.getLogo(agencyId);
        });
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Verifica che l'utente abbia accesso all'agenzia specificata
     */
    private void validateAgencyAccess(AgencyUserHub userHub, Long agencyId) {
        // Verifica che l'utente hub abbia almeno un AgencyUser associato a questa agenzia
        boolean hasAccess = userHub.getAgencyUsers().stream()
                .anyMatch(au -> au.getAgency() != null && au.getAgency().getId().equals(agencyId));
        
        if (!hasAccess) {
            throw new SecurityException("Non hai accesso a questa agenzia");
        }
    }
}
