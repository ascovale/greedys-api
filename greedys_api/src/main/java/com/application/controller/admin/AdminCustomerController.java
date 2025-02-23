package com.application.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.application.service.CustomerService;
import com.application.web.dto.AllergyDTO;
import com.application.web.dto.get.UserDTO;
import com.application.web.util.GenericResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/admin/user")
@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin User", description = "Admin management APIs for the User")
public class AdminCustomerController {
    private final CustomerService userService;

    @Autowired
    public AdminCustomerController(CustomerService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Create allergy", description = "Creates a new allergy for the specified user by their ID")
    @ApiResponse(responseCode = "200", description = "Allergy created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/allergy/createAllergy")
    public GenericResponse createAllergy(@RequestBody AllergyDTO allergyDto) {
        userService.createAllergy(allergyDto);
        return new GenericResponse("Allergy created successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Delete allergy", description = "Deletes an allergy by its ID")
    @ApiResponse(responseCode = "200", description = "Allergy deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @DeleteMapping("/allergy/deleteAllergy/{idAllergy}")
    public GenericResponse deleteAllergy(@PathVariable Long idAllergy) {
        userService.deleteAllergy(idAllergy);
        return new GenericResponse("Allergy deleted successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Modify allergy", description = "Modifies an existing allergy")
    @ApiResponse(responseCode = "200", description = "Allergy modified successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/allergy/{idAllergy}/modifyAllergy")
    public GenericResponse modifyAllergy(@PathVariable Long idAllergy, @RequestBody AllergyDTO allergyDto) {
        userService.modifyAllergy(idAllergy, allergyDto);
        return new GenericResponse("Allergy modified successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Block user", description = "Blocks a user by their ID")
    @ApiResponse(responseCode = "200", description = "User blocked successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/blockUser/{userId}")
    public GenericResponse blockUser(@PathVariable Long userId) {
        userService.blockUser(userId);
        return new GenericResponse("User blocked successfully");
    }

    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_WRITE')")
    @Operation(summary = "Enable user", description = "Enables a user by their ID")
    @ApiResponse(responseCode = "200", description = "User enabled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenericResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping("/enableUser/{userId}")
    public GenericResponse enableUser(@PathVariable Long userId) {
        userService.enableUser(userId);
        return new GenericResponse("User enabled successfully");
    }
    /*
     * @PreAuthorize("hasRole('ADMIN')")
     * 
     * @Operation(summary = "Remove user permissions", description =
     * "Removes permissions from a user by their ID")
     * 
     * @ApiResponse(responseCode = "200", description =
     * "Permissions removed successfully", content = @Content(mediaType =
     * "application/json", schema = @Schema(implementation =
     * GenericResponse.class)))
     * 
     * @ApiResponse(responseCode = "400", description = "Invalid request")
     * 
     * @PutMapping("/{idUser}/removePermissions")
     * public GenericResponse removePermissions(@PathVariable Long idUser) {
     * userService.removePermissions(idUser);
     * return new GenericResponse("Permissions removed successfully");
     * }
     */
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @GetMapping("/customers")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users";
    }
    @PreAuthorize("hasAuthority('PRIVILEGE_ADMIN_CUSTOMER_READ')")
    @Operation(summary = "List users with pagination", description = "Returns a paginated list of users")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(mediaType = "application/json"))
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @GetMapping("/customers/page")
    public Page<UserDTO> listUsersWithPagination(@RequestParam int page, @RequestParam int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return userService.findAll(pageable);
    }

}
