package com.application.admin.controller.restaurant;

import java.util.Collection;

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

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.service.RestaurantCategoryService;
import com.application.common.web.dto.ApiResponse;
import com.application.common.web.dto.RestaurantCategoryDTO;

import io.swagger.v3.oas.annotations.Operation;
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
public class AdminCategoryController extends BaseController {

	private final RestaurantCategoryService restaurantCategoryService;

	@GetMapping(value = "{restaurantId}/categories")
	@Operation(summary = "Get types of a restaurant", description = "Retrieve the types of a restaurant")
	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@ReadApiResponses
	public ResponseEntity<ApiResponse<Collection<String>>> getRestaurantTypesNames(@PathVariable Long restaurantId) {
		return execute("get restaurant types", () -> restaurantCategoryService.getRestaurantTypesNames(restaurantId));
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Create category", description = "Create a new category for the specified restaurant")
	@CreateApiResponses
	@PostMapping("/category/new")
	public ResponseEntity<ApiResponse<String>> createCategory(@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		return executeCreate("create category", "Category created successfully", () -> {
			restaurantCategoryService.createRestaurantCategory(restaurantCategoryDto);
			return "Category created successfully";
		});
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Delete category", description = "Delete a category by its ID")
	@DeleteMapping("/category/{categoryId}/delete")
	public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long categoryId) {
		return executeVoid("delete category", "Category deleted successfully", () -> {
			restaurantCategoryService.deleteRestaurantCategory(categoryId);
		});
	}

	@PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_RESTAURANT_WRITE')")
	@Operation(summary = "Update category", description = "Update an existing category by its ID")
	@PutMapping("/category/{categoryId}/update")
	public ResponseEntity<ApiResponse<String>> updateCategory(@PathVariable Long categoryId,
			@RequestBody RestaurantCategoryDTO restaurantCategoryDto) {
		return executeVoid("update category", "Category updated successfully", () -> {
			restaurantCategoryService.updateRestaurantCategory(categoryId, restaurantCategoryDto);
		});
	}
}
