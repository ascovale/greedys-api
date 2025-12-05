package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.CourseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DAO per CourseItem - Piatti nelle portate
 * 
 * Nota: L'entity CourseItem usa "available" invece di "isActive"
 */
@Repository
public interface CourseItemDAO extends JpaRepository<CourseItem, Long> {

    List<CourseItem> findByMenuCourseIdOrderByDisplayOrderAsc(Long courseId);

    /**
     * Trova items disponibili per una portata
     */
    List<CourseItem> findByMenuCourseIdAndAvailableTrue(Long courseId);

    /**
     * Trova items disponibili per una portata ordinati
     */
    @Query("SELECT i FROM CourseItem i WHERE i.menuCourse.id = :courseId " +
           "AND i.available = true ORDER BY i.displayOrder ASC")
    List<CourseItem> findAvailableByCourseOrdered(@Param("courseId") Long courseId);

    /**
     * Trova items vegetariani disponibili
     */
    @Query("SELECT i FROM CourseItem i WHERE i.menuCourse.id = :courseId " +
           "AND i.available = true AND (i.isVegetarian = true OR (i.dish IS NOT NULL AND i.dish.isVegetarian = true))")
    List<CourseItem> findVegetarianItems(@Param("courseId") Long courseId);

    /**
     * Trova items senza glutine disponibili
     */
    @Query("SELECT i FROM CourseItem i WHERE i.menuCourse.id = :courseId " +
           "AND i.available = true AND (i.isGlutenFree = true OR (i.dish IS NOT NULL AND i.dish.isGlutenFree = true))")
    List<CourseItem> findGlutenFreeItems(@Param("courseId") Long courseId);

    /**
     * Trova items collegati a un Dish
     */
    List<CourseItem> findByDishId(Long dishId);

    /**
     * Conta items disponibili per una portata
     */
    @Query("SELECT COUNT(i) FROM CourseItem i WHERE i.menuCourse.id = :courseId AND i.available = true")
    Long countAvailableByCourse(@Param("courseId") Long courseId);

    @Query("SELECT MAX(i.displayOrder) FROM CourseItem i WHERE i.menuCourse.id = :courseId")
    Integer findMaxDisplayOrder(@Param("courseId") Long courseId);
}
