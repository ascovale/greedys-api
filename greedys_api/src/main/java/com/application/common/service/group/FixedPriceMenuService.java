package com.application.common.service.group;

import com.application.common.persistence.dao.group.CourseItemDAO;
import com.application.common.persistence.dao.group.FixedPriceMenuDAO;
import com.application.common.persistence.dao.group.MenuCourseDAO;
import com.application.common.persistence.model.group.CourseItem;
import com.application.common.persistence.model.group.FixedPriceMenu;
import com.application.common.persistence.model.group.MenuCourse;
import com.application.common.persistence.model.group.enums.BookerType;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.menu.DishDAO;
import com.application.restaurant.persistence.model.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service per la gestione dei Menù a Prezzo Fisso.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FixedPriceMenuService {

    private final FixedPriceMenuDAO fixedPriceMenuDAO;
    private final MenuCourseDAO menuCourseDAO;
    private final CourseItemDAO courseItemDAO;
    private final RestaurantDAO restaurantDAO;
    private final DishDAO dishDAO;

    // ==================== CRUD MENU ====================

    public FixedPriceMenu createMenu(Long restaurantId, FixedPriceMenu menu) {
        Restaurant restaurant = restaurantDAO.findById(restaurantId)
            .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
        
        menu.setRestaurant(restaurant);
        menu.setEnabled(true);
        
        log.info("Creating fixed price menu '{}' for restaurant {}", menu.getName(), restaurantId);
        return fixedPriceMenuDAO.save(menu);
    }

    public FixedPriceMenu updateMenu(Long menuId, FixedPriceMenu updates) {
        FixedPriceMenu menu = getMenuById(menuId);
        
        if (updates.getName() != null) menu.setName(updates.getName());
        if (updates.getDescription() != null) menu.setDescription(updates.getDescription());
        if (updates.getBasePrice() != null) menu.setBasePrice(updates.getBasePrice());
        if (updates.getAgencyPrice() != null) menu.setAgencyPrice(updates.getAgencyPrice());
        if (updates.getMinimumPax() != null) menu.setMinimumPax(updates.getMinimumPax());
        if (updates.getMaximumPax() != null) menu.setMaximumPax(updates.getMaximumPax());
        if (updates.getMenuType() != null) menu.setMenuType(updates.getMenuType());
        if (updates.getVisibility() != null) menu.setVisibility(updates.getVisibility());
        if (updates.getValidFrom() != null) menu.setValidFrom(updates.getValidFrom());
        if (updates.getValidTo() != null) menu.setValidTo(updates.getValidTo());
        
        log.info("Updated fixed price menu {}", menuId);
        return fixedPriceMenuDAO.save(menu);
    }

    public void deleteMenu(Long menuId) {
        FixedPriceMenu menu = getMenuById(menuId);
        menu.setEnabled(false);
        fixedPriceMenuDAO.save(menu);
        log.info("Soft deleted menu {}", menuId);
    }

    public FixedPriceMenu getMenuById(Long menuId) {
        return fixedPriceMenuDAO.findById(menuId)
            .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuId));
    }

    // ==================== QUERY MENU ====================

    public List<FixedPriceMenu> getActiveMenusByRestaurant(Long restaurantId) {
        return fixedPriceMenuDAO.findActiveByRestaurantOrdered(restaurantId);
    }

    public Page<FixedPriceMenu> getMenusByRestaurant(Long restaurantId, Pageable pageable) {
        return fixedPriceMenuDAO.findByRestaurantId(restaurantId, pageable);
    }

    /**
     * Menù visibili ai Customer
     */
    public List<FixedPriceMenu> getCustomerMenus(Long restaurantId) {
        return fixedPriceMenuDAO.findCustomerMenus(restaurantId);
    }

    /**
     * Menù visibili alle Agency
     */
    public List<FixedPriceMenu> getAgencyMenus(Long restaurantId) {
        return fixedPriceMenuDAO.findAgencyMenus(restaurantId);
    }

    /**
     * Menù per tipo di booker con filtri
     */
    public List<FixedPriceMenu> getMenusForBooker(Long restaurantId, BookerType bookerType, Integer pax, LocalDate date) {
        List<FixedPriceMenu> menus = bookerType == BookerType.AGENCY 
            ? fixedPriceMenuDAO.findAgencyMenus(restaurantId)
            : fixedPriceMenuDAO.findCustomerMenus(restaurantId);
        
        return menus.stream()
            .filter(m -> pax == null || m.isPaxAllowed(pax))
            .filter(m -> date == null || m.isCurrentlyValid())
            .toList();
    }

    /**
     * Ricerca menù
     */
    public Page<FixedPriceMenu> searchMenus(Long restaurantId, String search, Pageable pageable) {
        return fixedPriceMenuDAO.searchMenus(restaurantId, search, pageable);
    }

    // ==================== CRUD COURSE ====================

    public MenuCourse addCourse(Long menuId, MenuCourse course) {
        FixedPriceMenu menu = getMenuById(menuId);
        
        Integer maxOrder = menuCourseDAO.findMaxDisplayOrder(menuId);
        course.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 1);
        course.setFixedPriceMenu(menu);
        
        log.info("Adding course '{}' to menu {}", course.getDisplayName(), menuId);
        return menuCourseDAO.save(course);
    }

    public MenuCourse updateCourse(Long courseId, MenuCourse updates) {
        MenuCourse course = menuCourseDAO.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        
        if (updates.getName() != null) course.setName(updates.getName());
        if (updates.getDescription() != null) course.setDescription(updates.getDescription());
        if (updates.getCourseType() != null) course.setCourseType(updates.getCourseType());
        if (updates.getSelectionType() != null) course.setSelectionType(updates.getSelectionType());
        if (updates.getSelectionCount() != null) course.setSelectionCount(updates.getSelectionCount());
        if (updates.getDisplayOrder() != null) course.setDisplayOrder(updates.getDisplayOrder());
        if (updates.getOptional() != null) course.setOptional(updates.getOptional());
        
        return menuCourseDAO.save(course);
    }

    public void deleteCourse(Long courseId) {
        menuCourseDAO.deleteById(courseId);
        log.info("Deleted course {}", courseId);
    }

    public List<MenuCourse> getCoursesByMenu(Long menuId) {
        return menuCourseDAO.findByFixedPriceMenuIdOrderByDisplayOrderAsc(menuId);
    }

    // ==================== CRUD COURSE ITEM ====================

    public CourseItem addCourseItem(Long courseId, CourseItem item) {
        MenuCourse course = menuCourseDAO.findById(courseId)
            .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        
        Integer maxOrder = courseItemDAO.findMaxDisplayOrder(courseId);
        item.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 1);
        item.setMenuCourse(course);
        item.setAvailable(true);
        
        // Se collegato a un Dish esistente
        if (item.getDish() != null && item.getDish().getId() != null) {
            item.setDish(dishDAO.getReferenceById(item.getDish().getId()));
        }
        
        log.info("Adding item '{}' to course {}", item.getName(), courseId);
        return courseItemDAO.save(item);
    }

    public CourseItem updateCourseItem(Long itemId, CourseItem updates) {
        CourseItem item = courseItemDAO.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Course item not found: " + itemId));
        
        if (updates.getCustomName() != null) item.setCustomName(updates.getCustomName());
        if (updates.getCustomDescription() != null) item.setCustomDescription(updates.getCustomDescription());
        if (updates.getSupplementPrice() != null) item.setSupplementPrice(updates.getSupplementPrice());
        if (updates.getAvailable() != null) item.setAvailable(updates.getAvailable());
        if (updates.getIsVegetarian() != null) item.setIsVegetarian(updates.getIsVegetarian());
        if (updates.getIsVegan() != null) item.setIsVegan(updates.getIsVegan());
        if (updates.getIsGlutenFree() != null) item.setIsGlutenFree(updates.getIsGlutenFree());
        if (updates.getIsLactoseFree() != null) item.setIsLactoseFree(updates.getIsLactoseFree());
        if (updates.getIsHalal() != null) item.setIsHalal(updates.getIsHalal());
        if (updates.getDisplayOrder() != null) item.setDisplayOrder(updates.getDisplayOrder());
        
        return courseItemDAO.save(item);
    }

    public void deleteCourseItem(Long itemId) {
        courseItemDAO.deleteById(itemId);
        log.info("Deleted course item {}", itemId);
    }

    public List<CourseItem> getItemsByCourse(Long courseId) {
        return courseItemDAO.findAvailableByCourseOrdered(courseId);
    }

    /**
     * Trova items con specifiche esigenze alimentari
     */
    public List<CourseItem> getVegetarianItems(Long courseId) {
        return courseItemDAO.findVegetarianItems(courseId);
    }

    public List<CourseItem> getGlutenFreeItems(Long courseId) {
        return courseItemDAO.findGlutenFreeItems(courseId);
    }

    // ==================== PRICING ====================

    /**
     * Calcola prezzo per un booker
     */
    public BigDecimal calculatePrice(Long menuId, BookerType bookerType, Integer pax) {
        FixedPriceMenu menu = getMenuById(menuId);
        BigDecimal pricePerPerson = bookerType == BookerType.AGENCY && menu.getAgencyPrice() != null 
            ? menu.getAgencyPrice() 
            : menu.getBasePrice();
        return pricePerPerson.multiply(BigDecimal.valueOf(pax));
    }

    /**
     * Verifica se il menu ha sconto agency
     */
    public boolean hasAgencyDiscount(Long menuId) {
        FixedPriceMenu menu = getMenuById(menuId);
        return menu.getAgencyPrice() != null && menu.getAgencyPrice().compareTo(menu.getBasePrice()) < 0;
    }

    /**
     * Copia un menu esistente (utile per creare varianti)
     */
    public FixedPriceMenu duplicateMenu(Long menuId, String newName) {
        FixedPriceMenu original = getMenuById(menuId);
        
        FixedPriceMenu copy = FixedPriceMenu.builder()
            .restaurant(original.getRestaurant())
            .name(newName != null ? newName : original.getName() + " (copia)")
            .description(original.getDescription())
            .basePrice(original.getBasePrice())
            .agencyPrice(original.getAgencyPrice())
            .minimumPax(original.getMinimumPax())
            .maximumPax(original.getMaximumPax())
            .menuType(original.getMenuType())
            .visibility(original.getVisibility())
            .enabled(true)
            .build();
        
        copy = fixedPriceMenuDAO.save(copy);
        
        // Copia courses e items
        for (MenuCourse origCourse : menuCourseDAO.findByFixedPriceMenuIdOrderByDisplayOrderAsc(menuId)) {
            MenuCourse courseCopy = MenuCourse.builder()
                .fixedPriceMenu(copy)
                .name(origCourse.getName())
                .description(origCourse.getDescription())
                .courseType(origCourse.getCourseType())
                .selectionType(origCourse.getSelectionType())
                .displayOrder(origCourse.getDisplayOrder())
                .selectionCount(origCourse.getSelectionCount())
                .optional(origCourse.getOptional())
                .build();
            courseCopy = menuCourseDAO.save(courseCopy);
            
            for (CourseItem origItem : courseItemDAO.findAvailableByCourseOrdered(origCourse.getId())) {
                CourseItem itemCopy = CourseItem.builder()
                    .menuCourse(courseCopy)
                    .dish(origItem.getDish())
                    .customName(origItem.getCustomName())
                    .customDescription(origItem.getCustomDescription())
                    .supplementPrice(origItem.getSupplementPrice())
                    .displayOrder(origItem.getDisplayOrder())
                    .isVegetarian(origItem.getIsVegetarian())
                    .isVegan(origItem.getIsVegan())
                    .isGlutenFree(origItem.getIsGlutenFree())
                    .isLactoseFree(origItem.getIsLactoseFree())
                    .isHalal(origItem.getIsHalal())
                    .available(true)
                    .build();
                courseItemDAO.save(itemCopy);
            }
        }
        
        log.info("Duplicated menu {} to new menu {}", menuId, copy.getId());
        return copy;
    }
}
