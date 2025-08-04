package com.application.persistence.dao.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.application.persistence.model.notification.RestaurantNotification;

public interface RestaurantNotificationDAO extends JpaRepository<RestaurantNotification, Long>{
    @Query("""
            SELECT n FROM RestaurantNotification n
            WHERE n.RUser.id = :userId AND n.isRead = false
            """)
    Page<RestaurantNotification> findByUserAndUnreadPagable(Long userId, Pageable pageable);

    @Query("""
            SELECT n FROM RestaurantNotification n
            WHERE n.RUser.id = :userId AND n.isRead = true
            """)
    Page<RestaurantNotification> findByUserAndReadPagable(Long userId, Pageable pageable);

    @Query("""
            SELECT n FROM RestaurantNotification n
            WHERE n.RUser.id = :userId
            """)
    Page<RestaurantNotification> findByUserPagable(Long userId, Pageable pageable);

}