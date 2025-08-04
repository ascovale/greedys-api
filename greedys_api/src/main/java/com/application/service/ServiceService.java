package com.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.customer.ReservationDAO;
import com.application.persistence.dao.restaurant.RUserDAO;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.model.reservation.Reservation;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.persistence.model.restaurant.user.RUser;
import com.application.web.dto.ServiceDto;
import com.application.web.dto.ServiceSlotsDto;
import com.application.web.dto.ServiceTypeDto;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.post.NewServiceDTO;
import com.application.web.dto.post.admin.AdminNewServiceDTO;
import com.application.web.dto.post.restaurant.RestaurantNewServiceDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@org.springframework.stereotype.Service
public class ServiceService {

	@Autowired
	ServiceDAO serviceDAO;
	@Autowired
	ServiceTypeDAO serviceTypeDAO;
	@Autowired
	ReservationDAO reservationDAO;
	@Autowired
	SlotDAO slotDAO;
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private RestaurantDAO rDAO;


	@Autowired
	private RUserDAO RUserDAO;

	@Autowired
	RestaurantService rService;

	public List<ServiceDto> getServices(Long idRestaurant, LocalDate selectedDate) {
		List<Service> services = serviceDAO.findServicesByRestaurant(idRestaurant);
		List<ServiceDto> servicesDto = new ArrayList<ServiceDto>();
		for (Service service : services) {
			servicesDto.add(new ServiceDto(service));
		}
		return servicesDto;
	}

	public Set<Weekday> getAllAvailableDays(Long idRestaurant) {
		List<Service> services = rService.findById(idRestaurant).getServices();
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

	public List<ServiceDto> getDayServicesFromWeekday(Restaurant restaurant, int weekday) {
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
		.restaurant(rService.findById(newServiceDTO.getRestaurant()))
		.validFrom(newServiceDTO.getValidFrom())
		.validTo(newServiceDTO.getValidTo())
		.active(false)
		.build();

		if (newServiceDTO.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			serviceTypes.add(entityManager.getReference(ServiceType.class, newServiceDTO.getServiceType()));
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}
		serviceDAO.save(service);
	}

	// TODO: Don't know why don't just create the slots while creating the service?
	@Transactional
	private void save(Service service, Set<Slot> slots) {
		service.setSlots(slots);
		slotDAO.saveAll(slots);
		serviceDAO.save(service);
	}

	public List<ServiceTypeDto> getServiceTypes() {
		return serviceTypeDAO.findAll().stream().map(ServiceTypeDto::new).toList();
	}

	public List<ServiceSlotsDto> getServiceSlots(Long idRUser, LocalDate date) {
		Long idRestaurant = RUserDAO.findById(idRUser).get().getRestaurant().getId();
		List<Service> services = serviceDAO.findServicesByRestaurant(idRestaurant);
		List<ServiceSlotsDto> servicesSlotsDto = new ArrayList<ServiceSlotsDto>();
		for (Service service : services) {
			servicesSlotsDto.add(new ServiceSlotsDto(service));
		}
		return servicesSlotsDto;
	}

	public List<ServiceDto> getDayServices(Restaurant restaurant, LocalDate date) {
		List<Service> services = serviceDAO.findServicesByRestaurant(restaurant.getId());
		List<ServiceDto> servicesDto = new ArrayList<>();
		for (Service service : services) {
			if (service.getValidFrom().isBefore(date) && service.getValidTo().isAfter(date)) {
				servicesDto.add(new ServiceDto(service));
			}
		}
		return servicesDto;
	}

	public ServiceDTO findById(Long serviceId) {
		return new ServiceDTO(serviceDAO.findById(serviceId).get());
	}

	public Service getById(Long serviceId) {
		return serviceDAO.findById(serviceId).get();
	}

	public void newService(AdminNewServiceDTO newServiceDTO) {
		Restaurant restaurant = rService.findById(newServiceDTO.getRestaurant());
		if (restaurant == null) {
			throw new IllegalArgumentException("Restaurant not found");
		}
		Service service = Service.builder()
			.name(newServiceDTO.getName())
			.restaurant(restaurant)
			.validFrom(newServiceDTO.getValidFrom())
			.validTo(newServiceDTO.getValidTo())
			.active(false)
			.build();

		if (newServiceDTO.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			serviceTypes.add(entityManager.getReference(ServiceType.class, newServiceDTO.getServiceType()));
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}
		serviceDAO.save(service);
		
	}

	public void newService(Long idRestaurant, RestaurantNewServiceDTO newServiceDTO) {
		Restaurant restaurant = rService.findById(idRestaurant);
		if (restaurant == null) {
			throw new IllegalArgumentException("Restaurant not found");
		}
		Service service = Service.builder()
		.name(newServiceDTO.getName())
		.restaurant(rService.findById(restaurant.getId()))
		.validFrom(newServiceDTO.getValidFrom())
		.validTo(newServiceDTO.getValidTo())
		.active(false)
		.build();

		if (newServiceDTO.getServiceType() != null) {
			Set<ServiceType> serviceTypes = new HashSet<>();
			serviceTypes.add(entityManager.getReference(ServiceType.class, newServiceDTO.getServiceType()));
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}
		serviceDAO.save(service);
	}


	// TODO: considerare il fatto di annullare le prenotazioni per un servizio e notificare l'utente
	@Transactional
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
				//reservation.setCancelUser(getCurrentUser());
				reservationDAO.save(reservation);
			}
		}
	}

	@Transactional
    public Collection<ServiceTypeDto> getServiceTypesFromRUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof RUser)) {
			throw new IllegalArgumentException("User is not a RUser");
		}
		RUser RUser = (RUser) principal;
		if (RUser.getRestaurant() == null) {
			throw new IllegalArgumentException("Restaurant not found");
		}
		Restaurant restaurant = rDAO.findById(RUser.getRestaurant().getId()).orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
		
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
		ServiceType serviceType = serviceTypeDAO.findById(typeId).orElseThrow(() -> new IllegalArgumentException("Service type not found"));
		serviceType.setName(serviceTypeString);
		serviceTypeDAO.save(serviceType);
	}

	public void deleteServiceType(Long typeId) {
		ServiceType serviceType = serviceTypeDAO.findById(typeId).orElseThrow(() -> new IllegalArgumentException("Service type not found"));
		serviceType.setDeleted(true);
		serviceTypeDAO.save(serviceType);
	}

	public void newService(RestaurantNewServiceDTO servicesDto) {
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
			serviceTypes.add(entityManager.getReference(ServiceType.class, servicesDto.getServiceType()));
			service.setServiceTypes(serviceTypes);
		} else {
			service.setServiceTypes(null);
		}

		service.setValidFrom(servicesDto.getValidFrom());
		service.setValidTo(servicesDto.getValidTo());
		service.setActive(false);
		serviceDAO.save(service);
	}

	

}
