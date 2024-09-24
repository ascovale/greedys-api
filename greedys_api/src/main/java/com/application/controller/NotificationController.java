package com.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.application.service.NotificationService;
import com.application.service.ReservationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.application.persistence.model.user.User;

@Controller
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ReservationService reservationService;
    
	@RequestMapping(value={"/index","/homepage"},method = RequestMethod.GET)
	public String getNotifications(Model model) {
		/*List<NotificationDto> notifications = notificationService.findByUser(getCurrentUser());
		if (notifications ==null) {
			notifications = new ArrayList<NotificationDto>();
		}	
		model.addAttribute("notifications", notifications);
		//System.out.println("conteggio notifiche = "+ notificationService.countNotification(getCurrentUser()));
		model.addAttribute("countNotification", notificationService.countNotification(getCurrentUser()));
		*/return "/index.html";
	}
	

	@RequestMapping(value="/notification/{id}", method = RequestMethod.GET)
    public String getNotification(@PathVariable Long id,Model model) {
    	model.addAttribute("notification",notificationService.getDto(id));
    	notificationService.read(id);
		return "/notification-details.html";
	}
	
	@RequestMapping(value="/notifications/read", method = RequestMethod.POST)
	public ResponseEntity<?> getNotificationsRead(Model model) {
		System.out.println("---->>>>> Siiiii  <<<<<-----");/* 
		notificationService.readNotification(getCurrentUser());*/
		return ResponseEntity.ok().build();
	}
	@RequestMapping(value="/notification-list", method = RequestMethod.GET)
	public String getNotificationList(Model model) {/* 
		List<NotificationDto> notifications = notificationService.findByUser(getCurrentUser());
		if (notifications ==null) {
			notifications = new ArrayList<NotificationDto>();
		}
		System.out.println("---->>>>> 2Siiiii2  <<<<<-----");
		System.out.println("conteggio notifiche = "+ notifications.size());
		model.addAttribute("notifications", notifications);*/
		return "/notification-list.html";
	}
	
	@RequestMapping(value="/notification4",method = RequestMethod.GET)
	public String getNotifications4(Model model) {/* 
		List<NotificationDto> notifications = notificationService..findByUser(getCurrentUser());
		if (notifications ==null) {
			notifications = new ArrayList<NotificationDto>();
		}	
		model.addAttribute("notifications", notifications);
		
		System.out.println("conteggio notifiche = "+ notificationService.countNotification(getCurrentUser()));
		for (NotificationDto n : notifications) {
			System.out.println("id = "+n.getId());
		}
		model.addAttribute("countNotification", notificationService.countNotification(getCurrentUser()));*/
		return "/notification4.html";
	}
	
	@RequestMapping(value="/notification2",method = RequestMethod.GET)
	public String getNotifications2(Model model) {/* 
		List<NotificationDto> notifications = notificationService.findByUser(getCurrentUser());
		if (notifications ==null) {
			notifications = new ArrayList<NotificationDto>();
		}	
		model.addAttribute("notifications", notifications);
		
		System.out.println("conteggio notifiche = "+ notificationService.countNotification(getCurrentUser()));
		for (NotificationDto n : notifications) {
			System.out.println("id = "+n.getId());
		}
		model.addAttribute("countNotification", notificationService.countNotification(getCurrentUser()));
		*/return "/notification2.html";
	}
	
	
	@RequestMapping(value="/send-notification", method = RequestMethod.POST)
	public ResponseEntity<?> sendMessage(@RequestParam Long idReservation,@RequestParam Long idUser, Model model) {
		notificationService.sendProvaNotification(idUser,idReservation);
		return ResponseEntity.ok().build();
	}
	
	private User getCurrentUser() {
		
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof User) {
			return ((User) principal);
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}

}
