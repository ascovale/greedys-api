package com.application.common.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.RestaurantGalleryImage;
import com.application.common.persistence.model.RestaurantGalleryImage.ImageType;

/**
 * Repository per la gestione delle immagini della galleria del ristorante
 */
@Repository
public interface RestaurantGalleryImageDAO extends JpaRepository<RestaurantGalleryImage, Long> {

    // ==================== FIND BY RESTAURANT ====================
    
    /**
     * Trova tutte le immagini attive di un ristorante ordinate per displayOrder
     */
    List<RestaurantGalleryImage> findByRestaurantIdAndIsActiveTrueOrderByDisplayOrderAsc(Long restaurantId);

    /**
     * Trova tutte le immagini di un ristorante per tipo
     */
    List<RestaurantGalleryImage> findByRestaurantIdAndImageTypeOrderByDisplayOrderAsc(Long restaurantId, ImageType imageType);

    /**
     * Trova le immagini della galleria di un ristorante (escluso logo e cover)
     */
    @Query("SELECT i FROM RestaurantGalleryImage i WHERE i.restaurant.id = :restaurantId " +
           "AND i.imageType = 'GALLERY' AND i.isActive = true ORDER BY i.displayOrder ASC")
    List<RestaurantGalleryImage> findGalleryImagesByRestaurantId(@Param("restaurantId") Long restaurantId);

    // ==================== FIND SINGLE IMAGES ====================
    
    /**
     * Trova il logo del ristorante
     */
    @Query("SELECT i FROM RestaurantGalleryImage i WHERE i.restaurant.id = :restaurantId " +
           "AND i.imageType = 'LOGO' AND i.isActive = true")
    Optional<RestaurantGalleryImage> findLogoByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Trova l'immagine di copertina del ristorante
     */
    @Query("SELECT i FROM RestaurantGalleryImage i WHERE i.restaurant.id = :restaurantId " +
           "AND i.imageType = 'COVER' AND i.isActive = true")
    Optional<RestaurantGalleryImage> findCoverByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Trova l'immagine featured del ristorante
     */
    @Query("SELECT i FROM RestaurantGalleryImage i WHERE i.restaurant.id = :restaurantId " +
           "AND i.isFeatured = true AND i.isActive = true ORDER BY i.displayOrder ASC")
    List<RestaurantGalleryImage> findFeaturedImagesByRestaurantId(@Param("restaurantId") Long restaurantId);

    // ==================== COUNT QUERIES ====================
    
    /**
     * Conta le immagini della galleria di un ristorante
     */
    @Query("SELECT COUNT(i) FROM RestaurantGalleryImage i WHERE i.restaurant.id = :restaurantId " +
           "AND i.imageType = 'GALLERY' AND i.isActive = true")
    long countGalleryImagesByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Verifica se esiste già un logo per il ristorante
     */
    boolean existsByRestaurantIdAndImageTypeAndIsActiveTrue(Long restaurantId, ImageType imageType);

    // ==================== UPDATE QUERIES ====================
    
    /**
     * Disattiva tutte le immagini di un certo tipo per un ristorante
     * (utile quando si carica un nuovo logo/cover)
     */
    @Modifying
    @Query("UPDATE RestaurantGalleryImage i SET i.isActive = false " +
           "WHERE i.restaurant.id = :restaurantId AND i.imageType = :imageType")
    int deactivateByRestaurantIdAndType(@Param("restaurantId") Long restaurantId, 
                                        @Param("imageType") ImageType imageType);

    /**
     * Aggiorna l'ordine di visualizzazione di un'immagine
     */
    @Modifying
    @Query("UPDATE RestaurantGalleryImage i SET i.displayOrder = :displayOrder WHERE i.id = :imageId")
    int updateDisplayOrder(@Param("imageId") Long imageId, @Param("displayOrder") Integer displayOrder);

    /**
     * Resetta il flag featured per tutte le immagini di un ristorante
     */
    @Modifying
    @Query("UPDATE RestaurantGalleryImage i SET i.isFeatured = false " +
           "WHERE i.restaurant.id = :restaurantId AND i.isFeatured = true")
    int resetFeaturedForRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * Trova il prossimo displayOrder disponibile per un ristorante
     */
    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) + 1 FROM RestaurantGalleryImage i " +
           "WHERE i.restaurant.id = :restaurantId AND i.imageType = 'GALLERY'")
    Integer findNextDisplayOrder(@Param("restaurantId") Long restaurantId);

    // ==================== DELETE QUERIES ====================
    
    /**
     * Soft delete - disattiva l'immagine
     */
    @Modifying
    @Query("UPDATE RestaurantGalleryImage i SET i.isActive = false WHERE i.id = :imageId")
    int softDeleteById(@Param("imageId") Long imageId);

    /**
     * Elimina tutte le immagini inattive più vecchie di X giorni (per pulizia)
     */
    @Modifying
    @Query("DELETE FROM RestaurantGalleryImage i WHERE i.isActive = false " +
           "AND i.updatedAt < :cutoffDate")
    int deleteInactiveOlderThan(@Param("cutoffDate") java.time.Instant cutoffDate);
}
