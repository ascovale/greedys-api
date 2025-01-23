package com.application.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.user.User;

@Service
public abstract class INotificationService<T> {
    public abstract List<T> newReservationNotification(Reservation reservation);
    public abstract void modifyReservationNotification(Reservation reservation);
    public abstract void deleteReservationNotification(Reservation reservation);

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
