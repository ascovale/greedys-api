package com.application.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.RestaurantImage;
import com.application.web.dto.RestaurantFullDetailsDto;
import com.application.web.dto.RestaurantImageDto;
import com.application.web.dto.ServiceDto;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.dto.get.RestaurantDTO;
import com.fasterxml.jackson.annotation.JsonCreator;

public class Mapper {
    public enum Weekday {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    @JsonCreator
    public static Weekday forValue(String value) {
        return Weekday.valueOf(value.toUpperCase());
    }
}
    /**
     * Transforms the list of {@link Restaurant} objects given as a method parameter
     * into a list of {@link RestaurantDTO} objects and returns the created list.
     *
     * @param entities
     * @return
     */
    public static List<RestaurantDTO> mapEntitiesIntoDTOs(Iterable<Restaurant> restaurants) {
        List<RestaurantDTO> dtos = new ArrayList<>();
        restaurants.forEach(e -> dtos.add(toDTO(e)));
        return dtos;
    }
    /**
     * Transforms the list of {@link Restaurant} objects given as a method parameter
     * into a list of {@link RestaurantDTO} objects and returns the created list.
     *
     * @param entities
     * @return
     */
    public static List<RestaurantImageDto> mapRestaurantImageIntoDTOs(Iterable<RestaurantImage> restaurantImages) {
        List<RestaurantImageDto> dtos = new ArrayList<>();
        restaurantImages.forEach(e -> dtos.add(toDTO(e)));
        return dtos;
    }

    /**
     * Transforms the {@link Restaurant} object given as a method parameter into a
     * {@link RestaurantDTO} object and returns the created object.
     *
     * @param entity
     * @return
     */
    public static RestaurantDTO toDTO(Restaurant r) {
        RestaurantDTO dto = new RestaurantDTO(r);
        return dto;
    }
    
    public static CustomerDTO toDTO(Customer r) {
        CustomerDTO dto = new CustomerDTO(r);
        return dto;
    }
    public static ServiceDto toDTO(Service service) {
        ServiceDto dto = new ServiceDto(service);
        return dto;
    }
    
    /**
     * Transforms the {@link Restaurant} object given as a method parameter into a
     * {@link RestaurantDTO} object and returns the created object.
     *
     * @param entity
     * @return
     */
    public static RestaurantImageDto toDTO(RestaurantImage ri) {
        RestaurantImageDto dto = new RestaurantImageDto();
        dto.setName(ri.getName());
        return dto;
    }
    
    public static RestaurantFullDetailsDto detailsToDTO(Restaurant r) {
        RestaurantFullDetailsDto dto = new RestaurantFullDetailsDto();
        dto.setId(r.getId());
        dto.setAddress(r.getAddress());
        dto.setEmail(r.getEmail());
        dto.setName(r.getName());
        dto.setpI(r.getPI());
        dto.setDescription(r.getDescription());
        dto.setPost_code(r.getPostCode());
       // dto.setRestaurantLogoDto(new RestaurantLogoDto(r.getRestaurantLogo().getPath()));
        return dto;
    }

    /**
     * Transforms {@code Page<ENTITY>} objects into {@code Page<DTO>} objects.
     * @param pageRequest   The information of the requested page.
     * @param source        The {@code Page<ENTITY>} object.
     * @return The created {@code Page<DTO>} object.
     */
    public static Page<RestaurantDTO> mapEntityPageIntoDTOPage(Pageable pageRequest, Page<Restaurant> source) {
        List<RestaurantDTO> dtos = mapEntitiesIntoDTOs(source.getContent());
        return new PageImpl<>(dtos, pageRequest, source.getTotalElements());
    }

    /**
     * Transforms {@code Page<ENTITY>} objects into {@code Page<DTO>} objects.
     * @param pageRequest   The information of the requested page.
     * @param source        The {@code Page<ENTITY>} object.
     * @return The created {@code Page<DTO>} object.
     */
    public static Page<RestaurantImageDto> mapRestaurantImagePageIntoDTOPage(Pageable pageRequest, Page<RestaurantImage> source) {
        List<RestaurantImageDto> dtos = mapRestaurantImageIntoDTOs(source.getContent());
        return new PageImpl<>(dtos, pageRequest, source.getTotalElements());
    }
}