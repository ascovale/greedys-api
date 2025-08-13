package com.application.restaurant.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.web.dto.service.AdminNewServiceDTO;
import com.application.common.persistence.mapper.Mapper.Weekday;
import com.application.common.persistence.mapper.ServiceDtoMapper;
import com.application.common.persistence.mapper.ServiceMapper;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.ServiceType;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.restaurant.ServiceDTO;
import com.application.common.web.dto.restaurant.ServiceSlotsDto;
import com.application.common.web.dto.restaurant.ServiceTypeDto;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.restaurant.persistence.dao.RUserDAO;
import com.application.restaurant.persistence.dao.RestaurantDAO;
import com.application.restaurant.persistence.dao.ServiceDAO;
import com.application.restaurant.persistence.dao.ServiceTypeDAO;
import com.application.restaurant.persistence.dao.SlotDAO;
import com.application.restaurant.persistence.model.Restaurant;
import com.application.restaurant.persistence.model.user.RUser;
import com.application.restaurant.web.dto.services.NewServiceDTO;
import com.application.restaurant.web.dto.services.RestaurantNewServiceDTO;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@Transactional
@RequiredArgsConstructor
public class ServiceService {

	private final ServiceDAO serviceDAO;
	private final ServiceTypeDAO serviceTypeDAO;
	private final ReservationDAO reservationDAO;
	private final SlotDAO slotDAO;
	private final RestaurantDAO rDAO;
	private final RUserDAO RUserDAO;
	private final ServiceMapper serviceMapper;
	private final ServiceDtoMapper serviceDtoMapper;
	

	public List<ServiceDTO> getServices(Long idRestaurant, LocalDate selectedDate) {
		List<Service> services = serviceDAO.findServicesByRestaurant(idRestaurant);
		return services.stream()
				.map(serviceDtoMapper::toDTO)
				.collect(Collectors.toList());
	}

	public Set<Weekday> getAllAvailableDays(Long idRestaurant) {
		Restaurant restaurant = rDAO.findById(idRestaurant).orElseThrow(() -> new IllegalArgumentException("Restaurant not found with ID: " + idRestaurant));
		List<Service> services = restaurant.getServices();
		Set<Weekday> days = new HashSet<Weekday>();

		for (Service service : services) {
			days.addAll(getAvailableDays(service.getId()));
		}
		return days;
	}

	public Set<Weekday> getAvailableDays(Long idService) {
		Set<Weekday> days = new HashSet<Weekday>();
		for (Slot slot : slotDAO.findByService_Id(idService)) {
			days.add(slot.getWeekday());
		}
		return days;
	}

	public List<ServiceDTO> getDayServicesFromWeekday(Restaurant restaurant, int weekday) {
		// DA CONTROLLARE QUI C'è QUALCHE ERRORE
		/*
		 * int adjustedWeekday = (weekday == 0) ? 6 : weekday - 1;
		 * Weekday weekdayEnum = Weekday.values()[adjustedWeekday];
		 * System.out.println("<<<   getDayServicesFromWeekday weekdayEnum: " +
		 * weekdayEnum);
		 * List<Service> list =
		 * serviceDAO.findServicesByWeekdayAndRestaurant(restaurant.getId(),
		 * weekdayEnum);
		 * List<ServiceDto> listDto = new ArrayList<ServiceDto>();
		 * for (Service service : list) {
		 * System.out.println("	<<<   list get: " + service.getName());
		 * System.out.println(service.getName());
		 * listDto.add(Mapper.toDTO(service));
		 * }
		 * return listDto;
		 */
		throw new UnsupportedOperationException("Unimplemented method 'getDayServicesFromWeekday'");
	}

	public void newService(NewServiceDTO newServiceDTO) {
		Service service = Service.builder()
				.name(newServiceDTO.getName())
				.restaurant(rDAO.findById(newServiceDTO.getRestaurant()).orElseThrow(() -> new IllegalArgumentException("Restaurant not found")))
				.validFrom(newServiceDTO.getValidFrom())
				.validTo(newServiceDTO.getValidTo())
				.active(false)
				.build();

		if (newServiceDTO.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			ServiceType serviceType = serviceTypeDAO.findById(newServiceDTO.getServiceType())
					.orElseThrow(() -> new IllegalArgumentException("Service type not found"));
			serviceTypes.add(serviceType);
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}
		serviceDAO.save(service);
	}

	public List<ServiceTypeDto> getServiceTypes() {
		return serviceTypeDAO.findAll().stream().map(ServiceTypeDto::new).toList();
	}

	public List<ServiceSlotsDto> getServiceSlots(Long idRUser, LocalDate date) {
		Long idRestaurant = RUserDAO.findById(idRUser).get().getRestaurant().getId();
		List<Service> services = serviceDAO.findServicesByRestaurant(idRestaurant);
		return services.stream()
				.map(service -> new ServiceSlotsDto(service))
				.collect(Collectors.toList());
	}

	public List<ServiceDTO> getDayServices(Restaurant restaurant, LocalDate date) {
		List<Service> services = serviceDAO.findServicesByRestaurant(restaurant.getId());
		return services.stream()
				.filter(service -> service.getValidFrom().isBefore(date) && service.getValidTo().isAfter(date))
				.map(serviceDtoMapper::toDTO)
				.collect(Collectors.toList());
	}

	public ServiceDTO findById(Long serviceId) {
		Service service = serviceDAO.findById(serviceId)
			.orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + serviceId));
		return serviceMapper.toDTO(service);
	}

	public Service getById(Long serviceId) {
		return serviceDAO.findById(serviceId).get();
	}

	public void newService(AdminNewServiceDTO newServiceDTO) {
		Restaurant restaurant = rDAO.findById(newServiceDTO.getRestaurant())
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
		Service service = Service.builder()
				.name(newServiceDTO.getName())
				.restaurant(restaurant)
				.validFrom(newServiceDTO.getValidFrom())
				.validTo(newServiceDTO.getValidTo())
				.active(false)
				.build();

		if (newServiceDTO.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			ServiceType serviceType = serviceTypeDAO.findById(newServiceDTO.getServiceType())
					.orElseThrow(() -> new IllegalArgumentException("Service type not found"));
			serviceTypes.add(serviceType);
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}
		serviceDAO.save(service);

	}

	public void newService(Long idRestaurant, RestaurantNewServiceDTO newServiceDTO) {
		Restaurant restaurant = rDAO.findById(idRestaurant).orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
		if (restaurant == null) {
			throw new IllegalArgumentException("Restaurant not found");
		}
		Service service = Service.builder()
				.name(newServiceDTO.getName())
				.restaurant(rDAO.findById(restaurant.getId()).orElseThrow(() -> new IllegalArgumentException("Restaurant not found")))
				.validFrom(newServiceDTO.getValidFrom())
				.validTo(newServiceDTO.getValidTo())
				.active(false)
				.build();

		if (newServiceDTO.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			ServiceType serviceType = serviceTypeDAO.findById(newServiceDTO.getServiceType())
					.orElseThrow(() -> new IllegalArgumentException("Service type not found"));
			serviceTypes.add(serviceType);
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}
		serviceDAO.save(service);
	}

	// TODO: considerare il fatto di annullare le prenotazioni per un servizio e
	// notificare l'utente
	public void deleteService(Long serviceId) {
		Service service = serviceDAO.findById(serviceId)
				.orElseThrow(() -> new IllegalArgumentException("Service not found"));
		service.setDeleted(true);
		serviceDAO.save(service);

		Collection<Slot> slots = slotDAO.findByService_Id(serviceId);
		for (Slot slot : slots) {
			slot.setDeleted(true);
			slotDAO.save(slot);
			Collection<Reservation> reservations = reservationDAO.findBySlot_Id(slot.getId());
			for (Reservation reservation : reservations) {
				reservation.setStatus(Reservation.Status.DELETED);
				// reservation.setCancelUser(getCurrentUser());
				reservationDAO.save(reservation);
			}
		}
	}

	public Collection<ServiceTypeDto> getServiceTypesFromRUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof RUser)) {
			throw new IllegalArgumentException("User is not a RUser");
		}
		RUser RUser = (RUser) principal;
		if (RUser.getRestaurant() == null) {
			throw new IllegalArgumentException("Restaurant not found");
		}
		Restaurant restaurant = rDAO.findById(RUser.getRestaurant().getId())
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

		Set<ServiceType> serviceTypes = new HashSet<>();
		for (Service service : restaurant.getServices()) {
			serviceTypes.addAll(service.getServiceTypes());
		}
		return serviceTypes.stream().map(ServiceTypeDto::new).toList();
	}

	public void newServiceType(String serviceTypeString) {
		ServiceType serviceType = new ServiceType();
		serviceType.setName(serviceTypeString);
		serviceTypeDAO.save(serviceType);
	}

	public void updateServiceType(Long typeId, String serviceTypeString) {
		ServiceType serviceType = serviceTypeDAO.findById(typeId)
				.orElseThrow(() -> new IllegalArgumentException("Service type not found"));
		serviceType.setName(serviceTypeString);
		serviceTypeDAO.save(serviceType);
	}

	public void deleteServiceType(Long typeId) {
		ServiceType serviceType = serviceTypeDAO.findById(typeId)
				.orElseThrow(() -> new IllegalArgumentException("Service type not found"));
		serviceType.setDeleted(true);
		serviceTypeDAO.save(serviceType);
	}

	public ServiceDTO newService(RestaurantNewServiceDTO servicesDto) {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof RUser)) {
			throw new IllegalArgumentException("User is not a RUser");
		}
		RUser RUser = (RUser) principal;
		Restaurant restaurant = RUser.getRestaurant();
		if (restaurant == null) {
			throw new IllegalArgumentException("Restaurant not found");
		}

		Service service = Service.builder()
				.name(servicesDto.getName())
				.restaurant(restaurant)
				.build();

		if (servicesDto.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			ServiceType serviceType = serviceTypeDAO.findById(servicesDto.getServiceType())
					.orElseThrow(() -> new IllegalArgumentException("Service type not found"));
			serviceTypes.add(serviceType);
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}

		service.setValidFrom(servicesDto.getValidFrom());
		service.setValidTo(servicesDto.getValidTo());
		service.setActive(false);
		service = serviceDAO.save(service);

		return new ServiceDTO(service);
	}

}
