package com.application.common.persistence.dao.group;

import com.application.common.persistence.model.group.MenuCourse;
import com.application.common.persistence.model.group.enums.CourseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DAO per MenuCourse - Portate del menù
 */
@Repository
public interface MenuCourseDAO extends JpaRepository<MenuCourse, Long> {

    List<MenuCourse> findByFixedPriceMenuIdOrderByDisplayOrderAsc(Long menuId);

    List<MenuCourse> findByFixedPriceMenuIdAndCourseType(Long menuId, CourseType courseType);

    @Query("SELECT COUNT(c) FROM MenuCourse c WHERE c.fixedPriceMenu.id = :menuId")
    Long countByMenu(@Param("menuId") Long menuId);

    @Query("SELECT MAX(c.displayOrder) FROM MenuCourse c WHERE c.fixedPriceMenu.id = :menuId")
    Integer findMaxDisplayOrder(@Param("menuId") Long menuId);
}
