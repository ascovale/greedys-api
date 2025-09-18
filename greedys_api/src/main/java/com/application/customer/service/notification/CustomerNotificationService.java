package com.application.customer.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.service.FirebaseService;
import com.application.common.web.dto.notification.CustomerNotificationDTO;
import com.application.common.web.dto.shared.NotificationDto;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.NotificationDAO;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.customer.service.CustomerFcmTokenService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerNotificationService {
    private final NotificationDAO notificationDAO;
    private final CustomerDAO userDAO;
    private final CustomerFcmTokenService customerFcmTokenService;
    private final FirebaseService firebaseService;

    public List<NotificationDto> findByUser(Customer user) {
        List<CustomerNotification> notifications = notificationDAO.findByCustomer(user);
        return notifications.stream()
                .map(notification -> new NotificationDto(
                    notification.getId(), 
                    notification.getCustomer().getId(), 
                    notification.getIsRead(),
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
        notification.setIsRead(true);
        notificationDAO.save(notification);
    }

    @Transactional
    public CustomerNotificationDTO markAsReadAndReturn(Long idNotification) {
        CustomerNotification notification = findById(idNotification)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        CustomerNotification savedNotification = notificationDAO.save(notification);
        return CustomerNotificationDTO.toDTO(savedNotification);
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
        return notificationDAO.findByCustomerAndIsReadFalse(getCurrentUser(), pageable);
    }

    @Transactional
    public Page<CustomerNotificationDTO> getUnreadNotificationsDTO(Pageable pageable) {
        Page<CustomerNotification> notifications = getUnreadNotifications(pageable);
        return notifications.map(CustomerNotificationDTO::toDTO);
    }

    //TODO da testare
    public Page<CustomerNotification> getAllNotifications(Pageable pageable) {
    Customer currentUser = getCurrentUser();
    if (!currentUser.isEnabled()) {
        throw new IllegalStateException("User is not enabled");
    }
        return notificationDAO.findAllByCustomer(getCurrentUser(), pageable);
    }

    public Page<CustomerNotificationDTO> getAllNotificationsDTO(Pageable pageable) {
        Page<CustomerNotification> notifications = getAllNotifications(pageable);
        return notifications.map(CustomerNotificationDTO::toDTO);
    }

    public void sendNotification(String title, String body, Map<String, String> data,  Long idCustomer) {
        List<String > tokens = customerFcmTokenService.getTokensByCustomerId(idCustomer).stream().map(t -> t.getFcmToken()).toList();
        firebaseService.sendNotification(title, body, data, tokens);
        
	}

}
