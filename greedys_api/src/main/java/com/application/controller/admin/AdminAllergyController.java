package com.application.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.service.UserService;
import com.application.web.dto.AllergyDTO;
import com.application.web.dto.RestaurantCategoryDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminAllergyController {
    private final RestaurantService restaurantService;
    private final UserService userService;


    public AdminAllergyController(UserService userService, RestaurantService restaurantService) {
        this.userService = userService;
        this.restaurantService = restaurantService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create allergy", description = "Crea una nuova allergia per l'utente specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Allergia creata con successo", 
                 content = @Content(mediaType = "application/json", 
                                    schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Richiesta non valida")
    @PostMapping("/createAllergy")
    public GenericResponse createAllergy(@RequestBody AllergyDTO allergyDto) {
        userService.createAllergy(allergyDto);
        return new GenericResponse("Allergy created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Crea una nuova categoria per il ristorante specificato tramite il suo ID")
    @ApiResponse(responseCode = "200", description = "Categoria creata con successo", 
                 content = @Content(mediaType = "application/json", 
                                    schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Richiesta non valida")
    @PostMapping("/createRestaurantCategory")
    public GenericResponse createCategory(@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
        restaurantService.createRestaurantCategory(restaurantCategoryDto);
        return new GenericResponse("Category created successfully");
    }
    //TODO fare i metodi addAllergy dove specifico id utente e removeAllergy idem del costumer Controller
}
