package com.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.NotificationDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Notification;
import com.application.persistence.model.customer.Notification.Type;
import com.application.persistence.model.reservation.Reservation;
import com.application.web.dto.NotificationDto;

@Service
public class NotificationService {
    private final NotificationDAO notificationDAO;
    private final CustomerDAO userDAO;
    private final FirebaseService firebaseService;

    public NotificationService(NotificationDAO notificationDAO, CustomerDAO userDAO, FirebaseService firebaseService) {
        this.notificationDAO = notificationDAO;
        this.userDAO = userDAO;
        this.firebaseService = firebaseService;
    }

    public Notification createReservationNotification(Reservation reservation,Type type) {
        Customer user = getCurrentUser();
        Notification notification = new Notification();
        notification.setCustomer(user);
        user.setToReadNotification(user.getToReadNotification() + 1);
        notification.setReservation(reservation);
        notification.setType(type);
        userDAO.save(user);
        notificationDAO.save(notification);
        firebaseService.sendFirebaseNotification(notification);
        return notification;
    }
  
    public List<NotificationDto> findByUser(Customer user) {
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
    public void readNotification(Customer currentUser) {
        Customer user = getCurrentUser();
        user.setToReadNotification(0);
        userDAO.save(user);    
    }
    
    public Integer countNotification(Customer currentUser) {
        Customer user = userDAO.findById(currentUser.getId()).get();    
        return user.getToReadNotification();
    }
    
    public void deleteNotification(long idNotification) {
        notificationDAO.deleteById(idNotification);
    }
    //getCurrentUser() è un metodo che ritorna l'utente corrente
    protected Customer getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Customer) {
            return ((Customer) principal);
        } else {
            System.out.println("Questo non dovrebbe succedere");
            return null;
        }
    }

    @Transactional
    public void setNotificationAsRead(Long notificationId, Boolean read) {
        Optional<Notification> notificationOpt = notificationDAO.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setUnopened(!read);
            notificationDAO.save(notification);
        }
    }

    @Transactional
    public Page<Notification> getUnreadNotifications(Pageable pageable) {
        return notificationDAO.findByUserAndUnopenedTrue(getCurrentUser(), pageable);
    }
    //TODO da testare
    public Page<Notification> getAllNotifications(Pageable pageable) {
    Customer currentUser = getCurrentUser();
    if (!currentUser.isEnabled()) {
        throw new IllegalStateException("User is not enabled");
    }
        return notificationDAO.findAllByCustomer(getCurrentUser(), pageable);
    }

    public void sendCustomerNotification(String title, String body, Long idCustomer) {
		firebaseService.sendFirebaseCustomerNotification(title, body, idCustomer);
	}

}
