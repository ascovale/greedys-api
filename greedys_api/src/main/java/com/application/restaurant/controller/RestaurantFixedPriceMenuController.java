package com.application.restaurant.controller;

import com.application.common.controller.BaseController;
import com.application.common.controller.annotation.CreateApiResponses;
import com.application.common.controller.annotation.ReadApiResponses;
import com.application.common.persistence.model.group.CourseItem;
import com.application.common.persistence.model.group.FixedPriceMenu;
import com.application.common.persistence.model.group.MenuCourse;
import com.application.common.persistence.model.group.enums.FixedPriceMenuType;
import com.application.common.persistence.model.group.enums.MenuVisibility;
import com.application.common.service.group.FixedPriceMenuService;
import com.application.restaurant.persistence.model.user.RUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller per la gestione dei Menù a Prezzo Fisso per gruppi.
 * Permette al ristoratore di creare e gestire menù con prezzi differenziati
 * per clienti diretti e agenzie.
 */
@RestController
@RequestMapping("/restaurant/group/menus")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Restaurant Fixed Price Menu", description = "Gestione menù a prezzo fisso per gruppi")
@RequiredArgsConstructor
@Slf4j
public class RestaurantFixedPriceMenuController extends BaseController {

    private final FixedPriceMenuService fixedPriceMenuService;

    // ==================== MENU CRUD ====================

    @Operation(summary = "Crea un nuovo menù a prezzo fisso")
    @CreateApiResponses
    @PostMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<FixedPriceMenu> createMenu(
            @AuthenticationPrincipal RUser user,
            @RequestBody CreateMenuRequest request) {
        
        return executeCreate("create fixed price menu", () -> {
            FixedPriceMenu menu = FixedPriceMenu.builder()
                .name(request.name())
                .description(request.description())
                .basePrice(request.basePrice())
                .agencyPrice(request.agencyPrice())
                .childrenPrice(request.childrenPrice())
                .minimumPax(request.minimumPax())
                .maximumPax(request.maximumPax())
                .visibility(request.visibility() != null ? request.visibility() : MenuVisibility.BOTH)
                .menuType(request.menuType() != null ? request.menuType() : FixedPriceMenuType.GROUP_STANDARD)
                .includesBeverages(request.includesBeverages())
                .beverageDescription(request.beverageDescription())
                .includesPrivateRoom(request.includesPrivateRoom())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();
            
            return fixedPriceMenuService.createMenu(user.getRestaurant().getId(), menu);
        });
    }

    @Operation(summary = "Ottieni tutti i menù del ristorante")
    @ReadApiResponses
    @GetMapping
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<Page<FixedPriceMenu>> getMenus(
            @AuthenticationPrincipal RUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return executePaginated("get fixed price menus", () ->
            fixedPriceMenuService.getMenusByRestaurant(user.getRestaurant().getId(), PageRequest.of(page, size)));
    }

    @Operation(summary = "Ottieni menù attivi del ristorante")
    @ReadApiResponses
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<List<FixedPriceMenu>> getActiveMenus(@AuthenticationPrincipal RUser user) {
        return executeList("get active menus", () ->
            fixedPriceMenuService.getActiveMenusByRestaurant(user.getRestaurant().getId()));
    }

    @Operation(summary = "Ottieni un menù specifico")
    @ReadApiResponses
    @GetMapping("/{menuId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<FixedPriceMenu> getMenu(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId) {
        
        return execute("get menu", () -> {
            FixedPriceMenu menu = fixedPriceMenuService.getMenuById(menuId);
            validateOwnership(user, menu);
            return menu;
        });
    }

    @Operation(summary = "Aggiorna un menù")
    @ReadApiResponses
    @PutMapping("/{menuId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<FixedPriceMenu> updateMenu(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId,
            @RequestBody UpdateMenuRequest request) {
        
        return execute("update menu", () -> {
            FixedPriceMenu menu = fixedPriceMenuService.getMenuById(menuId);
            validateOwnership(user, menu);
            
            FixedPriceMenu updates = FixedPriceMenu.builder()
                .name(request.name())
                .description(request.description())
                .basePrice(request.basePrice())
                .agencyPrice(request.agencyPrice())
                .childrenPrice(request.childrenPrice())
                .minimumPax(request.minimumPax())
                .maximumPax(request.maximumPax())
                .visibility(request.visibility())
                .menuType(request.menuType())
                .includesBeverages(request.includesBeverages())
                .beverageDescription(request.beverageDescription())
                .includesPrivateRoom(request.includesPrivateRoom())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();
            
            return fixedPriceMenuService.updateMenu(menuId, updates);
        });
    }

    @Operation(summary = "Elimina un menù (soft delete)")
    @ReadApiResponses
    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<Void> deleteMenu(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId) {
        
        return executeVoid("delete menu", () -> {
            FixedPriceMenu menu = fixedPriceMenuService.getMenuById(menuId);
            validateOwnership(user, menu);
            fixedPriceMenuService.deleteMenu(menuId);
        });
    }

    @Operation(summary = "Duplica un menù esistente")
    @CreateApiResponses
    @PostMapping("/{menuId}/duplicate")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<FixedPriceMenu> duplicateMenu(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId,
            @RequestParam(required = false) String newName) {
        
        return executeCreate("duplicate menu", () -> {
            FixedPriceMenu menu = fixedPriceMenuService.getMenuById(menuId);
            validateOwnership(user, menu);
            return fixedPriceMenuService.duplicateMenu(menuId, newName);
        });
    }

    // ==================== MENU QUERIES ====================

    @Operation(summary = "Menù per visibilità (CUSTOMER/AGENCY)")
    @ReadApiResponses
    @GetMapping("/by-visibility/{visibility}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<List<FixedPriceMenu>> getMenusByVisibility(
            @AuthenticationPrincipal RUser user,
            @PathVariable MenuVisibility visibility) {
        
        return executeList("get menus by visibility", () -> {
            Long restaurantId = user.getRestaurant().getId();
            return switch (visibility) {
                case CUSTOMER_ONLY, BOTH -> fixedPriceMenuService.getCustomerMenus(restaurantId);
                case AGENCY_ONLY -> fixedPriceMenuService.getAgencyMenus(restaurantId);
                default -> fixedPriceMenuService.getActiveMenusByRestaurant(restaurantId);
            };
        });
    }

    @Operation(summary = "Ricerca menù")
    @ReadApiResponses
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<Page<FixedPriceMenu>> searchMenus(
            @AuthenticationPrincipal RUser user,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return executePaginated("search menus", () ->
            fixedPriceMenuService.searchMenus(user.getRestaurant().getId(), query, PageRequest.of(page, size)));
    }

    // ==================== COURSES CRUD ====================

    @Operation(summary = "Aggiungi una portata al menù")
    @CreateApiResponses
    @PostMapping("/{menuId}/courses")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<MenuCourse> addCourse(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId,
            @RequestBody CreateCourseRequest request) {
        
        return executeCreate("add course", () -> {
            FixedPriceMenu menu = fixedPriceMenuService.getMenuById(menuId);
            validateOwnership(user, menu);
            
            MenuCourse course = MenuCourse.builder()
                .name(request.name())
                .description(request.description())
                .courseType(request.courseType())
                .selectionType(request.selectionType())
                .selectionCount(request.selectionCount())
                .optional(request.optional() != null ? request.optional() : false)
                .build();
            
            return fixedPriceMenuService.addCourse(menuId, course);
        });
    }

    @Operation(summary = "Ottieni portate di un menù")
    @ReadApiResponses
    @GetMapping("/{menuId}/courses")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<List<MenuCourse>> getCourses(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long menuId) {
        
        return executeList("get courses", () -> {
            FixedPriceMenu menu = fixedPriceMenuService.getMenuById(menuId);
            validateOwnership(user, menu);
            return fixedPriceMenuService.getCoursesByMenu(menuId);
        });
    }

    @Operation(summary = "Aggiorna una portata")
    @ReadApiResponses
    @PutMapping("/courses/{courseId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<MenuCourse> updateCourse(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long courseId,
            @RequestBody UpdateCourseRequest request) {
        
        return execute("update course", () -> {
            MenuCourse updates = MenuCourse.builder()
                .name(request.name())
                .description(request.description())
                .courseType(request.courseType())
                .selectionType(request.selectionType())
                .selectionCount(request.selectionCount())
                .displayOrder(request.displayOrder())
                .optional(request.optional())
                .build();
            
            return fixedPriceMenuService.updateCourse(courseId, updates);
        });
    }

    @Operation(summary = "Elimina una portata")
    @ReadApiResponses
    @DeleteMapping("/courses/{courseId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<Void> deleteCourse(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long courseId) {
        
        return executeVoid("delete course", () ->
            fixedPriceMenuService.deleteCourse(courseId));
    }

    // ==================== COURSE ITEMS CRUD ====================

    @Operation(summary = "Aggiungi un item a una portata")
    @CreateApiResponses
    @PostMapping("/courses/{courseId}/items")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<CourseItem> addCourseItem(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long courseId,
            @RequestBody CreateCourseItemRequest request) {
        
        return executeCreate("add course item", () -> {
            CourseItem item = CourseItem.builder()
                .customName(request.customName())
                .customDescription(request.customDescription())
                .supplementPrice(request.supplementPrice())
                .isVegetarian(request.isVegetarian())
                .isVegan(request.isVegan())
                .isGlutenFree(request.isGlutenFree())
                .isLactoseFree(request.isLactoseFree())
                .isHalal(request.isHalal())
                .isKosher(request.isKosher())
                .build();
            
            // Se riferito a un Dish esistente
            if (request.dishId() != null) {
                // Questo viene gestito nel service
            }
            
            return fixedPriceMenuService.addCourseItem(courseId, item);
        });
    }

    @Operation(summary = "Ottieni items di una portata")
    @ReadApiResponses
    @GetMapping("/courses/{courseId}/items")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<List<CourseItem>> getCourseItems(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long courseId) {
        
        return executeList("get course items", () ->
            fixedPriceMenuService.getItemsByCourse(courseId));
    }

    @Operation(summary = "Ottieni items vegetariani di una portata")
    @ReadApiResponses
    @GetMapping("/courses/{courseId}/items/vegetarian")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<List<CourseItem>> getVegetarianItems(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long courseId) {
        
        return executeList("get vegetarian items", () ->
            fixedPriceMenuService.getVegetarianItems(courseId));
    }

    @Operation(summary = "Ottieni items senza glutine di una portata")
    @ReadApiResponses
    @GetMapping("/courses/{courseId}/items/gluten-free")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_READ')")
    public ResponseEntity<List<CourseItem>> getGlutenFreeItems(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long courseId) {
        
        return executeList("get gluten free items", () ->
            fixedPriceMenuService.getGlutenFreeItems(courseId));
    }

    @Operation(summary = "Aggiorna un item")
    @ReadApiResponses
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<CourseItem> updateCourseItem(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long itemId,
            @RequestBody UpdateCourseItemRequest request) {
        
        return execute("update course item", () -> {
            CourseItem updates = CourseItem.builder()
                .customName(request.customName())
                .customDescription(request.customDescription())
                .supplementPrice(request.supplementPrice())
                .displayOrder(request.displayOrder())
                .available(request.available())
                .isVegetarian(request.isVegetarian())
                .isVegan(request.isVegan())
                .isGlutenFree(request.isGlutenFree())
                .isLactoseFree(request.isLactoseFree())
                .isHalal(request.isHalal())
                .isKosher(request.isKosher())
                .build();
            
            return fixedPriceMenuService.updateCourseItem(itemId, updates);
        });
    }

    @Operation(summary = "Elimina un item")
    @ReadApiResponses
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('PRIVILEGE_RESTAURANT_USER_MENU_WRITE')")
    public ResponseEntity<Void> deleteCourseItem(
            @AuthenticationPrincipal RUser user,
            @PathVariable Long itemId) {
        
        return executeVoid("delete course item", () ->
            fixedPriceMenuService.deleteCourseItem(itemId));
    }

    // ==================== HELPER ====================

    private void validateOwnership(RUser user, FixedPriceMenu menu) {
        if (!menu.getRestaurant().getId().equals(user.getRestaurant().getId())) {
            throw new IllegalArgumentException("Menu does not belong to this restaurant");
        }
    }

    // ==================== DTOs ====================

    public record CreateMenuRequest(
        String name,
        String description,
        BigDecimal basePrice,
        BigDecimal agencyPrice,
        BigDecimal childrenPrice,
        Integer minimumPax,
        Integer maximumPax,
        MenuVisibility visibility,
        FixedPriceMenuType menuType,
        Boolean includesBeverages,
        String beverageDescription,
        Boolean includesPrivateRoom,
        LocalDate validFrom,
        LocalDate validTo
    ) {}

    public record UpdateMenuRequest(
        String name,
        String description,
        BigDecimal basePrice,
        BigDecimal agencyPrice,
        BigDecimal childrenPrice,
        Integer minimumPax,
        Integer maximumPax,
        MenuVisibility visibility,
        FixedPriceMenuType menuType,
        Boolean includesBeverages,
        String beverageDescription,
        Boolean includesPrivateRoom,
        LocalDate validFrom,
        LocalDate validTo
    ) {}

    public record CreateCourseRequest(
        String name,
        String description,
        com.application.common.persistence.model.group.enums.CourseType courseType,
        com.application.common.persistence.model.group.enums.SelectionType selectionType,
        Integer selectionCount,
        Boolean optional
    ) {}

    public record UpdateCourseRequest(
        String name,
        String description,
        com.application.common.persistence.model.group.enums.CourseType courseType,
        com.application.common.persistence.model.group.enums.SelectionType selectionType,
        Integer selectionCount,
        Integer displayOrder,
        Boolean optional
    ) {}

    public record CreateCourseItemRequest(
        Long dishId,
        String customName,
        String customDescription,
        BigDecimal supplementPrice,
        Boolean isVegetarian,
        Boolean isVegan,
        Boolean isGlutenFree,
        Boolean isLactoseFree,
        Boolean isHalal,
        Boolean isKosher
    ) {}

    public record UpdateCourseItemRequest(
        String customName,
        String customDescription,
        BigDecimal supplementPrice,
        Integer displayOrder,
        Boolean available,
        Boolean isVegetarian,
        Boolean isVegan,
        Boolean isGlutenFree,
        Boolean isLactoseFree,
        Boolean isHalal,
        Boolean isKosher
    ) {}
}
