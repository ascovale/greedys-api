package com.application.customer.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.customer.persistence.dao.CustomerNotificationDAO;
import com.application.customer.persistence.model.CustomerNotification;
import com.application.common.persistence.model.notification.DeliveryStatus;
import com.application.common.service.FirebaseService;
import com.application.common.web.dto.notification.CustomerNotificationDTO;
import com.application.common.web.dto.shared.NotificationDto;
import com.application.customer.persistence.model.Customer;
import com.application.customer.service.CustomerFcmTokenService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerNotificationService {
    private final CustomerNotificationDAO notificationDAO;
    private final CustomerFcmTokenService customerFcmTokenService;
    private final FirebaseService firebaseService;

    public List<NotificationDto> findByUser(Customer user) {
        List<CustomerNotification> notifications = notificationDAO.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notifications.stream()
                .map(notification -> new NotificationDto(
                    notification.getId(), 
                    notification.getUserId(), 
                    notification.isRead(),
                    notification.getTitle(),
                    notification.getReadAt() != null ? notification.getReadAt() : notification.getCreationTime()

                )).toList();
    }

    public Optional<CustomerNotification> findById(Long id) {
        return notificationDAO.findById(id);
    }
    
    @Transactional
    public void read(Long idNotification) {
        CustomerNotification notification = findById(idNotification).get();
        notification.markAsRead();
        notificationDAO.save(notification);
    }

    @Transactional
    public CustomerNotificationDTO markAsReadAndReturn(Long idNotification) {
        CustomerNotification notification = findById(idNotification)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.markAsRead();
        CustomerNotification savedNotification = notificationDAO.save(notification);
        return CustomerNotificationDTO.toDTO(savedNotification);
    }
    
    public Integer countNotification(Customer currentUser) {
        long unreadCount = notificationDAO.countByUserIdAndStatus(currentUser.getId(), DeliveryStatus.PENDING);
        return (int) unreadCount;
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
    public Page<CustomerNotificationDTO> getUnreadNotificationsDTO(Pageable pageable) {
        Customer currentUser = getCurrentUser();
        List<CustomerNotification> unreadNotifications = notificationDAO.findUnreadByUserId(currentUser.getId());
        
        // Paginate the results manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), unreadNotifications.size());
        List<CustomerNotification> pageContent = unreadNotifications.subList(start, end);
        
        return new PageImpl<>(
            pageContent.stream().map(CustomerNotificationDTO::toDTO).toList(),
            pageable,
            unreadNotifications.size()
        );
    }

    public Page<CustomerNotificationDTO> getAllNotificationsDTO(Pageable pageable) {
        Customer currentUser = getCurrentUser();
        List<CustomerNotification> allNotifications = notificationDAO.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        
        // Paginate the results manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allNotifications.size());
        List<CustomerNotification> pageContent = allNotifications.subList(start, end);
        
        return new PageImpl<>(
            pageContent.stream().map(CustomerNotificationDTO::toDTO).toList(),
            pageable,
            allNotifications.size()
        );
    }

    public void sendNotification(String title, String body, Map<String, String> data,  Long idCustomer) {
        List<String > tokens = customerFcmTokenService.getTokensByCustomerId(idCustomer).stream().map(t -> t.getFcmToken()).toList();
        firebaseService.sendNotification(title, body, data, tokens);
	}

}

