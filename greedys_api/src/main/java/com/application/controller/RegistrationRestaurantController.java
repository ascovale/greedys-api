package com.application.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.persistence.dao.Restaurant.RestaurantRoleDAO;
import com.application.persistence.model.restaurant.RestaurantRole;
import com.application.persistence.model.restaurant.RestaurantUser;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.post.NewRestaurantDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/register/restaurant")
@SecurityRequirement(name = "bearerAuth")
public class RegistrationRestaurantController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestaurantRoleDAO restaurantRoleDAO;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantUserService restaurantUserService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    // Restaurant Registration
    @Operation(summary = "Register a new restaurant")
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Utente registrato con successo",
                    content = { @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NewRestaurantDTO.class)) }),
        }
    )
    @PostMapping(value = "/")
    public GenericResponse registerRestaurant(@RequestBody NewRestaurantDTO restaurantDto) {
        
    
        LOGGER.debug("Registering restaurant with information:", restaurantDto);
        System.out.println("Registering restaurant with information:" + restaurantDto.getName());
        RestaurantDTO r = restaurantService.registerRestaurant(restaurantDto);

       

        NewRestaurantUserDTO restaurantUserDTO = new NewRestaurantUserDTO();
        restaurantUserDTO.setRestaurantId(r.getId());
        restaurantUserDTO.setUserId(restaurantDto.getOwnerId());

        RestaurantUser owner = restaurantUserService.registerRestaurantUser(restaurantUserDTO);

        RestaurantRole rRole = new RestaurantRole();
        rRole.setName("ROLE_OWNER");
        rRole.setRestaurant(restaurantService.getReference(r.getId()));
        rRole.setUsers(Collections.singletonList(owner));
        restaurantRoleDAO.save(rRole);

        restaurantUserService.acceptUser(owner.getId());
        //eventPublisher.publishEvent(new UserOnRegistrationCompleteEvent(restaurantService.registerRestaurant(restaurantDto), request.getLocale(), getAppUrl(request)));
        return new GenericResponse("success");
    }

    @Operation(summary = "Apply for a restaurant")
    @PostMapping("/user")
    @ResponseBody
    public GenericResponse applyForRestaurant(@RequestBody NewRestaurantUserDTO userDTO) {
        
        restaurantUserService.registerRestaurantUser(userDTO);
        
        return new GenericResponse("success");
    }
    
    
}
