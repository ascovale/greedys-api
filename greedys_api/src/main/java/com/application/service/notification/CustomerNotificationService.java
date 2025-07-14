package com.application.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.NotificationDAO;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.notification.CustomerNotification;
import com.application.service.CustomerFcmTokenService;
import com.application.service.FirebaseService;
import com.application.web.dto.NotificationDto;

@Service
@Transactional
public class CustomerNotificationService {
    private final NotificationDAO notificationDAO;
    private final CustomerDAO userDAO;
    private final CustomerFcmTokenService customerFcmTokenService;
    private final FirebaseService firebaseService;

    public CustomerNotificationService(NotificationDAO notificationDAO, CustomerDAO userDAO, FirebaseService firebaseService) {
        this.notificationDAO = notificationDAO;
        this.userDAO = userDAO;
        this.customerFcmTokenService = null;
        this.firebaseService = firebaseService;
    }

    public List<NotificationDto> findByUser(Customer user) {
        List<CustomerNotification> notifications = notificationDAO.findByCustomer(user);
        return notifications.stream()
                .map(notification -> new NotificationDto(
                    notification.getId(), 
                    notification.getCustomer().getId(), 
                    notification.isRead(),
                    notification.getBody(),
                    notification.getCreationTime()

                )).toList();
    }

    public Optional<CustomerNotification> findById(Long id) {
        return notificationDAO.findById(id);
    }
    
    @Transactional
    public void read(Long idNotification) {
        CustomerNotification notification = findById(idNotification).get();
        notification.setRead(true);
        notificationDAO.save(notification);
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
    public Page<CustomerNotification> getUnreadNotifications(Pageable pageable) {
        return notificationDAO.findByCustomerAndReadFalse(getCurrentUser(), pageable);
    }

    //TODO da testare
    public Page<CustomerNotification> getAllNotifications(Pageable pageable) {
    Customer currentUser = getCurrentUser();
    if (!currentUser.isEnabled()) {
        throw new IllegalStateException("User is not enabled");
    }
        return notificationDAO.findAllByCustomer(getCurrentUser(), pageable);
    }

    public void sendNotification(String title, String body, Map<String, String> data,  Long idCustomer) {
        List<String > tokens = customerFcmTokenService.getTokensByCustomerId(idCustomer).stream().map(t -> t.getFcmToken()).toList();
        firebaseService.sendNotification(title, body, data, tokens);
        
	}

}
