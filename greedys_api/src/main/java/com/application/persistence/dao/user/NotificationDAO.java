package com.application.persistence.dao.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.user.User;
import com.application.persistence.model.user.Notification;

@Repository
public interface NotificationDAO extends JpaRepository<Notification, Long>{
	//@Query("SELECT * FROM Notification n WHERE n.iduser = ?1")
	public List<Notification> findByUser(User User);

    Page<Notification> findByUserAndUnopenedTrue(User user, Pageable pageable);

	public Page<Notification> findAllNotifications(Pageable pageable);
}
