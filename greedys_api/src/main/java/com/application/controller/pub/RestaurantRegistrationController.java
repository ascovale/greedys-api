package com.application.controller.pub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.service.RestaurantUserService;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.dto.post.NewRestaurantDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/public/register/restaurant")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantRegistrationController {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantUserService restaurantUserService;

    // Restaurant Registration

    @Operation(summary = "Register a new restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utente registrato con successo", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewRestaurantDTO.class)) }),
    })
    @PostMapping(value = "/")
    public GenericResponse registerRestaurant(@RequestBody NewRestaurantDTO restaurantDto) {
        LOGGER.debug("Registering restaurant with information:", restaurantDto);
        System.out.println("Registering restaurant with information:" + restaurantDto.getName());
        restaurantService.registerRestaurant(restaurantDto);
        return new GenericResponse("success");
    }

    @Operation(summary = "Register a new restaurant and user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ristorante e utente registrati con successo", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = NewRestaurantDTO.class)) }),
    })
    @PostMapping("/registerRestaurantAndUser")
    public GenericResponse registerRestaurantAndUser(@RequestBody NewRestaurantDTO restaurantDto,
            NewCustomerDTO accountDto, HttpServletRequest request) {
        LOGGER.debug("Registering restaurant with information:", restaurantDto);
        
        restaurantService.registerRestaurantAndUser(restaurantDto, accountDto);
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
