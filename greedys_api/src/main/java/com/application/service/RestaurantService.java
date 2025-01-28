package com.application.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.mapper.Mapper;
import com.application.persistence.dao.restaurant.RestaurantCategoryDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.model.Image;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantCategory;
import com.application.web.dto.RestaurantCategoryDTO;
import com.application.web.dto.RestaurantFullDetailsDto;
import com.application.web.dto.RestaurantImageDto;
import com.application.web.dto.get.RestaurantDTO;
import com.application.web.dto.get.RestaurantUserDTO;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.NewRestaurantDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;



@Transactional
@Service("restaurantService")
public class RestaurantService {

	@Autowired
	private RestaurantDAO rDAO;

	@Autowired
	private RestaurantUserDAO ruDAO;

	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private RestaurantCategoryDAO restaurantCategoryDAO;

	public Restaurant getReference(Long id) {
		return entityManager.getReference(Restaurant.class, id);
	}

	public Restaurant findById(Long id) {
		return this.rDAO.findById(id).get();
	}

	@Transactional
	public List<Restaurant> findAll() {
		return rDAO.findAll();
	}

	 
	public Restaurant findById(long i) {
		return this.rDAO.findById(i).get();
	}

	 
	public void save(Restaurant restaurant) {
		rDAO.save(restaurant);
	}

	 
	public Collection<RestaurantDTO> findBySearchTerm(String searchTerm) {
		Collection<Restaurant> searchResultPage = rDAO.findBySearchTerm(searchTerm);
		return searchResultPage.stream()
			.map(restaurant -> new RestaurantDTO(restaurant))
			.collect(Collectors.toList());
	}

	 
	public RestaurantDTO registerRestaurant(NewRestaurantDTO restaurantDto) {
		Restaurant restaurant = new Restaurant();
		restaurant.setEmail(restaurantDto.getEmail());
		restaurant.setName(restaurantDto.getName());
		restaurant.setAddress(restaurantDto.getAddress());
		restaurant.setCreationDate(LocalDate.now());
  		restaurant.setpI(restaurantDto.getpi());
		restaurant.setPostCode(restaurantDto.getPost_code());
		rDAO.save(restaurant);
		System.out.println("<<< ID= "+restaurant.getId());
		return new RestaurantDTO(restaurant);
	}

	 
	public RestaurantFullDetailsDto findByIdRestaurantFullDetails(Long idRestaurant) {
		 Restaurant r = rDAO.findByIdFullDetailsRestaurant(idRestaurant);
		 RestaurantFullDetailsDto rfd = Mapper.detailsToDTO(r);
		 return rfd;
	}
	 
	public Page<RestaurantImageDto> getImages(Long idRestaurant,Pageable pageable) {
		Page<RestaurantImageDto> rid=Mapper.mapRestaurantImagePageIntoDTOPage(pageable,rDAO.getRestaurantImages(idRestaurant,pageable));
		return rid;
	}
	 
	public void addImage(Restaurant restaurant, Image image){
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

	public Collection<RestaurantUserDTO> getRestaurantUsers(Long id) {
		return ruDAO.findByRestaurantId(id).stream()
			.map(r -> new RestaurantUserDTO(r))
			.collect(Collectors.toList());
	}

	public Collection<SlotDTO> getDaySlots(Long id, LocalDate date) {
		return rDAO.finDaySlots(id, date).stream()
			.map(s -> new SlotDTO(s))
			.collect(Collectors.toList());
	}

    public Collection<ServiceDTO> getServices(Long id) {
        return rDAO.findServices(id).stream()
			.map(s -> new ServiceDTO(s))
			.collect(Collectors.toList());
    }

	@Transactional
    public void setNowShowTimeLimit(Long idRestaurant, int minutes) {
		Restaurant restaurant = rDAO.findById(idRestaurant).orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		restaurant.setNoShowTimeLimit(minutes);
		rDAO.save(restaurant);
	}

    public List<String> getRestaurantTypesNames(Long idRestaurant) {
		Restaurant restaurant = rDAO.findById(idRestaurant).orElseThrow(() -> new IllegalArgumentException("Invalid restaurant ID"));
		 restaurant.getRestaurantTypes();
	return restaurant.getRestaurantTypes().stream()
		.map(RestaurantCategory::getName)
		.collect(Collectors.toList());
	}

	@Transactional
	public void createRestaurantCategory(RestaurantCategoryDTO restaurantCategoryDto) {
		RestaurantCategory restaurantCategory = new RestaurantCategory();
		restaurantCategory.setName(restaurantCategoryDto.getName());
		restaurantCategory.setDescription(restaurantCategoryDto.getDescription());
		restaurantCategoryDAO.save(restaurantCategory);
	}

}
