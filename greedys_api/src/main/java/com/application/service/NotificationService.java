package com.application.service;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.application.persistence.dao.user.NotificationDAO;
import com.application.persistence.dao.user.ReservationDAO;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.user.User;
import com.application.persistence.model.user.Notification;
import com.application.persistence.model.user.Notification.Type;
import com.application.web.dto.NotificationDto;
import com.application.persistence.model.user.UserFcmToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;

@Service
public class NotificationService extends INotificationService<Notification> {

    public static final String SECURED_CHAT_SPECIFIC_USER = "/secured/user/queue/specific-user";
    private static final String FIREBASE_API_URL = "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send";
    private GoogleCredentials googleCredentials;
    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;
    private final ReservationDAO reservationDAO;
    private final UserFcmTokenService userFcmTokenService;

    public NotificationService(NotificationDAO notificationDAO, UserDAO userDAO, ReservationDAO reservationDAO, UserFcmTokenService userFcmTokenService) {
        this.notificationDAO = notificationDAO;
        this.userDAO = userDAO;
        this.reservationDAO = reservationDAO;
        this.userFcmTokenService = userFcmTokenService;
        try {
            this.googleCredentials = GoogleCredentials.fromStream(new FileInputStream("/run/secrets/service_account"))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            this.googleCredentials.refreshIfExpired();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google Credentials: " + e.getMessage(), e);
        }
    }

    public Notification createNoShowNotification(Reservation reservation) {
        User user = super.getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user); 
        notification.setReservation(reservation);
        notification.setType(Type.NO_SHOW);
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "No Show", "You have missed your reservation.");
        return notification;
    }

    public Notification createSeatedNotification(Reservation reservation) {
        User user = super.getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user);
        notification.setReservation(reservation);
        notification.setType(Type.SEATED);
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "Seated", "You have been seated successfully.");
        return notification;
    }

    public void deleteReservationNotification(Reservation reservation) {
        User user = super.getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user);
        notification.setReservation(reservation);
        notification.setType(Type.CANCEL);
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "Reservation Cancelled", "Your reservation has been cancelled.");
    }

    public Notification createConfirmedNotification(Reservation reservation) {
        User user = super.getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user);
        notification.setReservation(reservation);
        notification.setType(Type.CONFIRMED);
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "Reservation Confirmed", "Your reservation has been confirmed.");
        return notification;
    }

    public NotificationDto createAlteredNotification(Reservation reservation) {
        User user = super.getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user);
        notification.setReservation(reservation);
        notification.setType(Type.ALTERED);
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "Reservation Altered", "Your reservation details have been altered.");
        return NotificationDto.toDto(notification);
    }
    
    public void sendProvaNotification(Long idUser, Long idReservation) {
        User user = userDAO.findById(idUser).get();
        Notification notification = new Notification();
        Reservation reservation = reservationDAO.findById(idReservation).get();
        notification.setClientUser(user);
        notification.setReservation(reservation);
        notification.setType(Type.ALTERED);
        notification.setText("Testo di una notifica di Prova");
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "Test Notification", "This is a test notification.");
    }

    @Override
    public void newReservationNotification(Reservation reservation) {
        User user = super.getCurrentUser();
        Notification notification = new Notification();
        notification.setClientUser(user);
        notification.setReservation(reservation);
        notification.setType(Type.NEW_RESERVATION);
        notificationDAO.save(notification);
        sendFirebaseNotification(user, "New Reservation", "Your reservation has been created by the restaurant.");
    }

    public void sendFirebaseNotification(User user, String title, String message) {
        List<UserFcmToken> tokens = userFcmTokenService.getTokensByUserId(user.getId());
        for (UserFcmToken token : tokens) {
            try {
                googleCredentials.refreshIfExpired();
                String accessToken = googleCredentials.getAccessToken().getTokenValue();

                RestTemplate restTemplate = new RestTemplate();

                // Set HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + accessToken);

                // Create notification body
                Map<String, Object> messageBody = new HashMap<>();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("token", token.getFcmToken());
                Map<String, String> notification = new HashMap<>();
                notification.put("title", title);
                notification.put("body", message);
                messageData.put("notification", notification);
                messageBody.put("message", messageData);

                // Convert to JSON
                Gson gson = new Gson();
                String jsonBody = gson.toJson(messageBody);

                // Create the request
                HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

                // Send the request to Firebase
                ResponseEntity<String> response = restTemplate.exchange(
                    FIREBASE_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
                );

                // Log the response (optional)
                System.out.println("Firebase response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error sending Firebase notification: " + e.getMessage());
            }
        }
    }

    public Optional<String> getOldTokenIfPresent(String deviceId) {
        UserFcmToken token = userFcmTokenService.getTokenByDeviceId(deviceId);
        return Optional.of(token.getFcmToken());
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

    @Override
    public void modifyReservationNotification(Reservation reservation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'modifyReservationNotification'");
    }

}
