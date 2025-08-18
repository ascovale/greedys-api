package com.application.common.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.Mapper;
import com.application.common.persistence.model.Image;
import com.application.common.web.dto.restaurant.RestaurantDTO;
import com.application.common.web.dto.restaurant.RestaurantFullDetailsDto;
import com.application.common.web.dto.restaurant.RestaurantImageDto;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.common.web.dto.restaurant.SlotDTO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RestaurantCategoryDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.RestaurantRoleDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.RestaurantCategory;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.persistence.model.user.RestaurantRole;
import com.application.restaurant.service.RUserService;
import com.application.restaurant.web.dto.restaurant.NewRestaurantDTO;
import com.application.restaurant.web.dto.staff.NewRUserDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class RestaurantService {
	
	private final RestaurantDAO rDAO;
	private final RUserDAO ruDAO;
	private final RestaurantRoleDAO restaurantRoleDAO;
	private final RUserService rUserService;
	private final ServiceDAO sDAO;
	private final RestaurantCategoryDAO rcDAO;

	public Restaurant getReference(Long id) {
		return rDAO.getReferenceById(id);
	}
	public RestaurantDTO findById(Long id) {
		Restaurant restaurant = this.rDAO.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + id));
		return new RestaurantDTO(restaurant);
	}

	public List<Restaurant> findAll() {
		return rDAO.findAll();
	}

	public Collection<RestaurantDTO> findAllDto() {
		return rDAO.findAll().stream()
			.map(RestaurantDTO::new)
			.collect(Collectors.toList());
	}

	public void save(Restaurant restaurant) {
		rDAO.save(restaurant);
	}

	public Collection<RestaurantDTO> findBySearchTerm(String searchTerm) {
		Collection<Restaurant> searchResultPage = rDAO.findBySearchTerm(searchTerm);
		return searchResultPage.stream()
				.map(RestaurantDTO::new)
				.collect(Collectors.toList());
	}

	public Page<RestaurantDTO> findAllPaginated(Pageable pageable) {
		return rDAO.findAll(pageable).map(RestaurantDTO::new);
	}

	public RestaurantDTO registerRestaurant(NewRestaurantDTO restaurantDto) {
		Restaurant restaurant = new Restaurant();
		restaurant.setEmail(restaurantDto.getEmail());
		restaurant.setName(restaurantDto.getName());
		restaurant.setAddress(restaurantDto.getAddress());
		restaurant.setCreationDate(LocalDate.now());
		restaurant.setVatNumber(restaurantDto.getVatNumber());
		restaurant.setPostCode(restaurantDto.getPost_code());
		restaurant.setStatus(Restaurant.Status.DISABLED);
		rDAO.save(restaurant);

		RestaurantRole role = restaurantRoleDAO.findByName("ROLE_OWNER");
		NewRUserDTO RUserDTO = NewRUserDTO.builder()
			.email(restaurant.getEmail())
			.password(restaurantDto.getPassword()) // FIX: Don't double-encode - let registerRUser handle encoding
			.firstName(restaurantDto.getOwnerName())
			.lastName(restaurantDto.getOwnerSurname())
			.roleId(role.getId())
			.build();
		
		RUser owner = rUserService.registerRUser(RUserDTO, restaurant);
		owner.setStatus(RUser.Status.ENABLED);
		ruDAO.save(owner);
		return new RestaurantDTO(restaurant);
	}

	public RestaurantFullDetailsDto findByIdRestaurantFullDetails(Long idRestaurant) {
		Restaurant restaurant = rDAO.findByIdFullDetailsRestaurant(idRestaurant);
		if (restaurant == null) {
			throw new IllegalArgumentException("Restaurant not found with ID: " + idRestaurant);
		}
		return Mapper.detailsToDTO(restaurant);
	}

	//TODO: Add restaurant user bisogna verificare che il ristorante esista

	public Page<RestaurantImageDto> getImages(Long idRestaurant, Pageable pageable) {
		return Mapper.mapRestaurantImagePageIntoDTOPage(pageable,
				rDAO.getRestaurantImages(idRestaurant, pageable));
	}

	public void addImage(Restaurant restaurant, Image image) {
		Hibernate.initialize(restaurant.getRestaurantImages());
		restaurant.getRestaurantImages().add(image);
		rDAO.save(restaurant);
	}

	public Collection<LocalDate> getClosedDays(Long id, LocalDate start, LocalDate end) {
		return rDAO.findClosedDays(id, start, end);
	}

	public Collection<String> getOpenDays(Long idRestaurant, LocalDate start, LocalDate end) {
		return rDAO.findOpenDaysInRange(idRestaurant, start, end).stream()
				.map(object -> (String) object)
				.collect(Collectors.toList());
	}

	public Collection<RUser> findRUsers(Long id) {
		return ruDAO.findByRestaurantId(id);
	}

	public Collection<SlotDTO> getDaySlots(Long id, LocalDate date) {
		return rDAO.finDaySlots(id, date).stream()
				.map(SlotDTO::new)
				.collect(Collectors.toList());
	}

	public Collection<ServiceDTO> getServices(Long id) {
		try {
			// Verify restaurant exists first with better error handling
			boolean exists = false;
			try {
				exists = rDAO.existsById(id);
			} catch (Exception e) {
				log.warn("Error checking restaurant existence for ID {}: {}", id, e.getMessage());
				// If existsById fails, assume restaurant doesn't exist
				exists = false;
			}
			
			if (!exists) {
				throw new com.application.common.web.error.RestaurantNotFoundException("Restaurant not found with ID: " + id);
			}
			
			return rDAO.findServices(id).stream()
					.map(ServiceDTO::new)
					.collect(Collectors.toList());
		} catch (com.application.common.web.error.RestaurantNotFoundException e) {
			// Re-throw the specific exception
			throw e;
		} catch (Exception e) {
			// Log the error and throw a more specific exception
			log.error("Error getting services for restaurant ID {}: {}", id, e.getMessage(), e);
			throw new com.application.common.web.error.RestaurantNotFoundException("Restaurant not found with ID: " + id);
		}
	}

	public void setNoShowTimeLimit(Long idRestaurant, int minutes) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setNoShowTimeLimit(minutes);
		rDAO.save(restaurant);
	}

	public List<String> getRestaurantTypesNames(Long idRestaurant) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		return restaurant.getRestaurantTypes().stream()
				.map(RestaurantCategory::getName)
				.collect(Collectors.toList());
	}

	public List<String> getRestaurantTypesNames() {
		RUser RUser = (RUser) SecurityContextHolder.getContext().getAuthentication()
				.getPrincipal();
		if (RUser != null) {
			Restaurant restaurant = RUser.getRestaurant();
			return restaurant.getRestaurantTypes().stream()
					.map(RestaurantCategory::getName)
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public void deleteRestaurant(Long idRestaurant) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setStatus(Restaurant.Status.DELETED);
		rDAO.save(restaurant);
	}


	public void changeRestaurantEmail(Long idRestaurant, String newEmail) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setEmail(newEmail);
		rDAO.save(restaurant);
	}

	public void enableRestaurant(Long idRestaurant) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setStatus(Restaurant.Status.ENABLED);
		rDAO.save(restaurant);
	}
	public void addRestaurantCategory(Long idRestaurant, Long categoryId) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		RestaurantCategory category = rcDAO.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
		restaurant.getRestaurantTypes().add(category);
		rDAO.save(restaurant);
	}

	public void removeRestaurantCategory(Long idRestaurant, Long categoryId) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		RestaurantCategory category = rcDAO.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid category ID"));
		restaurant.getRestaurantTypes().remove(category);
		rDAO.save(restaurant);
	}

	public void setRestaurantDeleted(Long idRestaurant, boolean b) {
		Restaurant restaurant = rDAO.findById(idRestaurant)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setStatus(Restaurant.Status.DELETED);
		rDAO.save(restaurant);
	}

	public void updateRestaurantStatus(Long restaurantId, Restaurant.Status newStatus) {
		Restaurant restaurant = rDAO.findById(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setStatus(newStatus);
		rDAO.save(restaurant);
		
		Collection<RUser> RUsers = ruDAO.findByRestaurantId(restaurantId);
		for (RUser ru : RUsers) {
			// Aggiorna lo stato del customer
			ruDAO.save(ru);
		}
	}

	public Collection<ServiceDTO> getActiveEnabledServices(Long restaurantId, LocalDate date) {
		return sDAO.findActiveEnabledServices(restaurantId, date).stream()
			.map(ServiceDTO::new)
			.collect(Collectors.toList());
	}

	public Collection<ServiceDTO> findActiveEnabledServicesInPeriod(Long restaurantId, LocalDate start, LocalDate end) {
		return sDAO.findActiveEnabledServicesInPeriod(restaurantId, start, end).stream()
			.map(ServiceDTO::new)
			.collect(Collectors.toList());
	}

	public Restaurant createRestaurant(RestaurantDTO restaurantDto) {
		Restaurant restaurant = new Restaurant();
		restaurant.setEmail(restaurantDto.getEmail());
		restaurant.setName(restaurantDto.getName());
		restaurant.setAddress(restaurantDto.getAddress());
		restaurant.setCreationDate(LocalDate.now());
		restaurant.setVatNumber(restaurantDto.getVatNumber());
		restaurant.setPostCode(restaurantDto.getPost_code());
		restaurant.setStatus(Restaurant.Status.DISABLED);
		rDAO.save(restaurant);
		return restaurant;
	}
	//TODO: Implement findAllPaginatedDisabled
	/* 
	public Page<RestaurantDTO> findAllPaginatedEnabled(Pageable pageable) {
		return rDAO.findByStatus(Restaurant.Status.ENABLED, pageable).map(RestaurantDTO::new);
	}*/
	
}
