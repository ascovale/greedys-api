package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.RestaurantService;
import com.application.web.dto.RestaurantCategoryDTO;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Restaurant", description = "Admin management APIs for the Restaurant")
public class AdminRestaurantController {

    private RestaurantService restaurantService;

    @Autowired
    public AdminRestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", description = "Creates a new category for the specified restaurant by its ID")
    @ApiResponse(responseCode = "200", description = "Category created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/createRestaurantCategory")
    public GenericResponse createCategory(@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
        restaurantService.createRestaurantCategory(restaurantCategoryDto);
        return new GenericResponse("Category created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Deletes a category by its ID")
    @ApiResponse(responseCode = "200", description = "Category deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/deleteRestaurantCategory/{idCategory}")
    public GenericResponse deleteCategory(@PathVariable Long idCategory) {
        restaurantService.deleteRestaurantCategory(idCategory);
        return new GenericResponse("Category deleted successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "Updates an existing category by its ID")
    @ApiResponse(responseCode = "200", description = "Category updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/updateRestaurantCategory/{idCategory}")
    public GenericResponse updateCategory(@PathVariable Long idCategory,
            @RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
        restaurantService.updateRestaurantCategory(idCategory, restaurantCategoryDto);
        return new GenericResponse("Category updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable restaurant", description = "Enables a restaurant by its primary email")
    @ApiResponse(responseCode = "200", description = "Restaurant enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/restaurant/{idRestaurant}/enableRestaurant")
    public GenericResponse enableRestaurant(@PathVariable Long idRestaurant) {
        restaurantService.enableRestaurant(idRestaurant);
        return new GenericResponse("Restaurant enabled successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete restaurant", description = "Deletes a restaurant by its ID")
    @ApiResponse(responseCode = "200", description = "Restaurant deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/restaurant/{idRestaurant}/deleteRestaurant")
    public GenericResponse deleteRestaurant(@PathVariable Long idRestaurant) {
        restaurantService.deleteRestaurant(idRestaurant);
        return new GenericResponse("Restaurant deleted successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create restaurant", description = "Creates a new restaurant")
    @ApiResponse(responseCode = "200", description = "Restaurant created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/createRestaurant")
    public GenericResponse createRestaurant(@RequestBody RestaurantDTO restaurantDto) {
        restaurantService.createRestaurant(restaurantDto);
        return new GenericResponse("Restaurant created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change restaurant email", description = "Changes the email of a restaurant by its ID")
    @ApiResponse(responseCode = "200", description = "Restaurant email changed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/restaurant/{idRestaurant}/changeEmail")
    public GenericResponse changeRestaurantEmail(@PathVariable Long idRestaurant, @RequestBody String newEmail) {
        restaurantService.changeRestaurantEmail(idRestaurant, newEmail);
        return new GenericResponse("Restaurant email changed successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark restaurant as deleted", description = "Marks a restaurant as deleted similar to disable by its ID")
    @ApiResponse(responseCode = "200", description = "Restaurant marked as deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/restaurant/{idRestaurant}/markAsDeleted")
    public GenericResponse markRestaurantAsDeleted(@PathVariable Long idRestaurant) {
        restaurantService.markRestaurantAsDeleted(idRestaurant);
        return new GenericResponse("Restaurant marked as deleted successfully");
    }

}
