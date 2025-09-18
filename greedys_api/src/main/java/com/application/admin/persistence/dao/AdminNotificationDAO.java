package com.application.admin.persistence.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.admin.persistence.model.Admin;
import com.application.admin.persistence.model.AdminNotification;

@Repository
public interface AdminNotificationDAO extends JpaRepository<AdminNotification, Long>{
	//@Query("SELECT * FROM Notification n WHERE n.idadmin = ?1")
	public List<AdminNotification> findByAdmin(Admin admin);

    //Page<AdminNotification> findByAdminAndReadFalse(Admin admin, Pageable pageable);

    public Page<AdminNotification> findAllByAdmin(Admin admin, Pageable pageable);
}
