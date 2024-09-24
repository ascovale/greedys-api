package com.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.application.persistence.dao.user.NotificationDAO;
import com.application.persistence.dao.user.ReservationDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.user.User;
import com.application.persistence.model.user.Notification;
import com.application.persistence.model.user.Notification.Type;
import com.application.web.dto.NotificationDto;

@Service
public class NotificationService {
	
    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";
    
    @Autowired
	NotificationDAO notificationDAO;
    @Autowired
	UserDAO userDAO;
    @Autowired
	SimpMessagingTemplate messagingTemplate;
    @Autowired
	ReservationDAO reservationDAO;
    

	public NotificationDto createNoShowNotification(User user,Reservation reservation) {
		Notification notification = new Notification();
		notification.setClientUser(user);
		notification.setReservation(reservation);
		notification.setType(Type.NO_SHOW);
		notificationDAO.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), SECURED_CHAT_SPECIFIC_USER, notification);
		return NotificationDto.toDto(notification);
	}

	public NotificationDto createSeatedNotification(User user,Reservation reservation) {
		Notification notification = new Notification();
		notification.setClientUser(user);
		notification.setReservation(reservation);
		notification.setType(Type.SEATED);
		notificationDAO.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), SECURED_CHAT_SPECIFIC_USER, notification);
		return NotificationDto.toDto(notification);
	}


	public NotificationDto createdCancelNotification(User user,Reservation reservation) {
		Notification notification = new Notification();
		notification.setClientUser(user);
		notification.setReservation(reservation);
		notification.setType(Type.CANCEL);
		notificationDAO.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), SECURED_CHAT_SPECIFIC_USER, notification);
		return NotificationDto.toDto(notification);
	}

	public NotificationDto createConfirmedNotification(User user,Reservation reservation) {
		Notification notification = new Notification();
		notification.setClientUser(user);
		notification.setReservation(reservation);
		notification.setType(Type.CONFIRMED);
		notificationDAO.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), SECURED_CHAT_SPECIFIC_USER, notification);
		return NotificationDto.toDto(notification);
	}

	public NotificationDto createAlteredNotification(User user,Reservation reservation) {
		Notification notification = new Notification();
		notification.setClientUser(user);
		notification.setReservation(reservation);
		notification.setType(Type.ALTERED);
		notificationDAO.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), SECURED_CHAT_SPECIFIC_USER, notification);
		return NotificationDto.toDto(notification);
	}
	

	public void sendProvaNotification(Long idUser, Long idReservation) {
		User user= userDAO.findById(idUser).get();
		Notification notification = new Notification();
		Reservation reservation = reservationDAO.findById(idReservation).get();
		notification.setClientUser(user);
		notification.setReservation(reservation);
		notification.setType(Type.ALTERED);
		notification.setText("Testo di una notifica di Prova");
		notificationDAO.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), SECURED_CHAT_SPECIFIC_USER, notification);
	}

	 
	public List<NotificationDto> findByUser(User user) {
		List<Notification> notifications = notificationDAO.findByUser(user);
		return NotificationDto.toDto(notifications);
	}

	 
	public Optional<Notification> findById(Long id) {
		return notificationDAO.findById(id);
	}
	
	 
	public NotificationDto getDto(Long id) {
		return NotificationDto.toDto(findById(id).get());
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

}
