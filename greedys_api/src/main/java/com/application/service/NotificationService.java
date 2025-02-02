package com.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.user.NotificationDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.user.Notification;
import com.application.persistence.model.user.Notification.Type;
import com.application.persistence.model.user.User;
import com.application.web.dto.NotificationDto;

@Service
public class NotificationService {
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final FirebaseService firebaseService;

    public NotificationService(NotificationDAO notificationDAO, UserDAO userDAO, FirebaseService firebaseService) {
        this.notificationDAO = notificationDAO;
        this.userDAO = userDAO;
        this.firebaseService = firebaseService;
    }

    public Notification createReservationNotification(Reservation reservation,Type type) {
        User user = getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user); 
        notification.setReservation(reservation);
        notification.setType(type);
        notificationDAO.save(notification);
        firebaseService.sendFirebaseNotification(notification);
        return notification;
    }
  
    public List<NotificationDto> findByUser(User user) {
        List<Notification> notifications = notificationDAO.findByUser(user);
        return NotificationDto.toDto(notifications);
    }

    public Optional<Notification> findById(Long id) {
        return notificationDAO.findById(id);
    }
    
    @Transactional
    public void read(Long idNotification) {
        Notification notification = findById(idNotification).get();
        notification.setUnopened(false);
        notificationDAO.save(notification);
    }
    
    @Transactional
    public void readNotification(User currentUser) {
        User user = userDAO.findById(currentUser.getId()).get();
        user.setToReadNotification((long) 0);
        userDAO.save(user);    
    }
    
    public long countNotification(User currentUser) {
        User user = userDAO.findById(currentUser.getId()).get();    
        return user.getToReadNotification();
    }
    
    public void deleteNotification(long idNotification) {
        notificationDAO.deleteById(idNotification);
    }

    protected User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

}
