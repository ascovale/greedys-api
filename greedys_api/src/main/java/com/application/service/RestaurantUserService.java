package com.application.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantRole;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;

@Service
@Transactional
public class RestaurantUserService {

    @Autowired
    private EmailService emailService;
    @Autowired
    private RestaurantUserDAO ruDAO;
    @Autowired
    private RestaurantDAO restaurantDAO;

    @Autowired
    private UserService userService;

    // TODO Io farei che l'user può essere creato nello stesso tempo
    // se già esiste, lo prendo, altrimenti lo creo
    // invio di email come se fosse utente normale
    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " "
                + restaurantUserDTO.getUserId());
        RestaurantUser ru = new RestaurantUser();
        Restaurant restaurant = restaurantDAO.findById(restaurantUserDTO.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid restaurant ID: " + restaurantUserDTO.getRestaurantId()));
        ru.setRestaurant(restaurant);
        ru.setUser(userService.getReference(restaurantUserDTO.getUserId()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }

    public void acceptUser(Long id) {
        ruDAO.acceptUser(id);
    }

    public Collection<RestaurantUserDTO> getRestaurantUsers(Long id) {
        return ruDAO.findByRestaurantId(id).stream().map(user -> new RestaurantUserDTO(user)).toList();
    }

    public void blockRestaurantUser(Long idRestaurantUser) {
        RestaurantUser ru = ruDAO.findById(idRestaurantUser)
                .orElseThrow(() -> new IllegalArgumentException("Invalid restaurant user ID: " + idRestaurantUser));
        ru.setBlocked(true);
        ruDAO.save(ru);
    }

    public void removeRestaurantUser(Long idRestaurantUser) {
        ruDAO.deleteById(idRestaurantUser);
    }

    public void changeRestaurantOwner(Long idRestaurant, Long idOldOwner, Long idNewOwner) {
        // VERIFICARE CHE L'ID DEL Restaurant USer sia quello del restaurant corretto
        // cioè che esista un restaurantUser che abbia i permessi ROLE_OWNER e abbia
        // quel ristorante
        RestaurantUser oldOwner = ruDAO.findById(idOldOwner)
                .orElseThrow(() -> new IllegalArgumentException("Invalid old owner ID: " + idOldOwner));
        RestaurantUser newOwner = ruDAO.findById(idNewOwner)
                .orElseThrow(() -> new IllegalArgumentException("Invalid new owner ID: " + idNewOwner));
        new RestaurantRole("ROLE_OWNER");
        oldOwner.setRole(new RestaurantRole("ROLE_USER"));
        newOwner.setRole(new RestaurantRole("ROLE_OWNER"));

    }

    public RestaurantUser registerRestaurantUser(NewRestaurantUserDTO restaurantUserDTO, Restaurant restaurant) {
        System.out.println("Registering restaurant user with information:" + restaurantUserDTO.getRestaurantId() + " "
                + restaurantUserDTO.getUserId());
        RestaurantUser ru = new RestaurantUser();
        ru.setRestaurant(restaurant);
        ru.setUser(userService.getReference(restaurantUserDTO.getUserId()));
        ruDAO.save(ru);
        emailService.sendRestaurantAssociationConfirmationEmail(ru);
        return ru;
    }
}