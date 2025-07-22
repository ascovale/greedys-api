package com.application.admin.controller.restaurant;

import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.web.dto.RestaurantCategoryDTO;
import com.application.common.web.util.GenericResponse;
import com.application.restaurant.service.RestaurantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequestMapping("/admin/restaurant")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Category", description = "Admin Category Management")
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

	private final RestaurantService restaurantService;

	@GetMapping(value = "{restaurantId}/categories")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Operation successful", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Restaurant not found"),
			@ApiResponse(responseCode = "400", description = "Invalid request"),
			@ApiResponse(responseCode = "401", description = "Unauthorized"),
			@ApiResponse(responseCode = "405", description = "Method not allowed"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	public ResponseEntity<Collection<String>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		List<String> types = restaurantService.getRestaurantTypesNames(restaurantId);
		return new ResponseEntity<>(types, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create category", description = "Create a new category for the specified restaurant")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Category created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PostMapping("/category/new")
	public GenericResponse createCategory(@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		restaurantService.createRestaurantCategory(restaurantCategoryDto);
		return new GenericResponse("Category created successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Delete category", description = "Delete a category by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Category deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@DeleteMapping("/category/{categoryId}/delete")
	public GenericResponse deleteCategory(@PathVariable Long categoryId) {
		restaurantService.deleteRestaurantCategory(categoryId);
		return new GenericResponse("Category deleted successfully");
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Update category", description = "Update an existing category by its ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Category updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class))),
		@ApiResponse(responseCode = "400", description = "Invalid request"),
		@ApiResponse(responseCode = "401", description = "Unauthorized"),
		@ApiResponse(responseCode = "405", description = "Method not allowed"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@PutMapping("/category/{categoryId}/update")
	public GenericResponse updateCategory(@PathVariable Long categoryId,
			@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		restaurantService.updateRestaurantCategory(categoryId, restaurantCategoryDto);
		return new GenericResponse("Category updated successfully");
	}
}
