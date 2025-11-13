package com.application.common.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.admin.persistence.model.AdminNotification;

/**
 * DAO per AdminNotification
 * 
 * ⚠️ NOTA: AdminNotification ha una struttura LEGACY diversa da ANotification
 * Non estende ANotification e ha campi type, admin, reservation, text
 * Le query custom sono disabilitate per evitare path exception in Hibernate
 * 
 * Per funzionalità complesse, usare derived query methods o native SQL
 */
public interface AdminNotificationDAO extends JpaRepository<AdminNotification, Long> {
    // Eredita findById, save, delete, etc da JpaRepository
    // Non aggiungere custom @Query methods poiché AdminNotification ha struttura legacy
}
