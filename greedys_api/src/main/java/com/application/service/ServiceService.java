package com.application.service;


import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.Restaurant.ServiceDAO;
import com.application.persistence.dao.Restaurant.ServiceTypeDAO;
import com.application.persistence.dao.Restaurant.SlotDAO;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.web.dto.ServiceDto;
import com.application.web.dto.ServiceSlotsDto;
import com.application.web.dto.ServiceTypeDto;
import com.application.web.dto.ServicesDto;
import com.application.web.dto.get.ServiceDTO;
import com.application.web.dto.post.NewServiceDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@org.springframework.stereotype.Service
public class ServiceService {
	
	@Autowired
	ServiceDAO serviceDAO;
	@Autowired
	ServiceTypeDAO serviceTypeDAO;
	@Autowired
	SlotDAO slotDAO;
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	RestaurantService rService;

	 
	public List<ServiceDto> getServices(Long idRestaurant, Date selectedDate) {
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


	/*
	 * ATTENZIONE!!
	 * La classe Calendar è obsoleta e non dovrebbe essere usata.
	 
	public List<ServiceDto> getDayServices(Restaurant restaurant, Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int weekday = calendar.get(Calendar.DAY_OF_WEEK);
		Weekday weekdayEnum = Weekday.values()[weekday - 1];
		List<Service> services = serviceDAO.findServicesByWeekdayAndRestaurant(restaurant.getId(), weekdayEnum);
		List<ServiceDto> servicesDto = new ArrayList<>();
		for (Service service : services) {
			servicesDto.add(Mapper.toDTO(service));
		}
		return servicesDto;
	}
	*/

	 
	public List<ServiceDto> getDayServicesFromWeekday(Restaurant restaurant, int weekday) {
		//DA CONTROLLARE QUI C'è QUALCHE ERRORE
		/*
		int adjustedWeekday = (weekday == 0) ? 6 : weekday - 1;
		Weekday weekdayEnum = Weekday.values()[adjustedWeekday];
		System.out.println("<<<   getDayServicesFromWeekday weekdayEnum: " + weekdayEnum);
		List<Service> list = serviceDAO.findServicesByWeekdayAndRestaurant(restaurant.getId(), weekdayEnum);
		List<ServiceDto> listDto = new ArrayList<ServiceDto>();
		for (Service service : list) {
			System.out.println("	<<<   list get: " + service.getName());
			System.out.println(service.getName());
			listDto.add(Mapper.toDTO(service));
		}
		return listDto;
		*/
		throw new UnsupportedOperationException("Unimplemented method 'getDayServicesFromWeekday'");
	}

	public void newService(NewServiceDTO newServiceDTO) {
		Service service = new Service();
		service.setName(newServiceDTO.getName());
		service.setRestaurant(rService.findById(newServiceDTO.getRestaurant()));

		if (newServiceDTO.getServiceType() != null) {
			service.setServiceType(entityManager.getReference(ServiceType.class, newServiceDTO.getServiceType()));
		} else {
			service.setServiceType(null);
		}
		
		serviceDAO.save(service);
	}
		

	 
	public void addService(ServicesDto servicesDto) {
		List<String> weekdaysStrings = servicesDto.getWeekdays();
		List<Weekday> weekdays = new ArrayList<>();
		for (String weekdayString : weekdaysStrings) {
			System.out.println("<<<   weekdayString: " + weekdayString);
			Weekday weekdayEnum = Weekday.valueOf(weekdayString.toUpperCase());
			weekdays.add(weekdayEnum);
		}
		ServiceType st = serviceTypeDAO.findById(servicesDto.getServiceType()).get();
		for (Weekday weekday : weekdays) {
			Service service = new Service();
			service.setName(servicesDto.getName());
			service.setRestaurant(rService.findById(servicesDto.getRestaurant()));
			service.setServiceType(st);
			serviceDAO.save(service);
			LocalTime timeReservation = servicesDto.getTimeReservation();
			LocalTime open = servicesDto.getOpen();
			LocalTime close = servicesDto.getClose();

			Set<Slot> slots = new HashSet<>();
			
			/* Il codice andrà sistemato perchè se si va al giorno dopo bisogna aggiungere anche il giorno dopo*/
			while (open.isBefore(close) || open.equals(close)) {
				System.out.print("	BEFORE   open: " + open);
				Slot slot = new Slot(open, close);
				slot.setService(service);
				slots.add(slot);
				open =open.plusHours(timeReservation.getHour()).plusMinutes(timeReservation.getMinute());
				System.out.println("	AFTER   openDateTime: " + open + "    HOUR = " +timeReservation.getHour()+ "    MINUTES = " +timeReservation.getMinute());
			}
			save(service, slots);
		}
	}

	@Transactional
	private void save(Service service, Set<Slot> slots){
		service.setSlots(slots);
		slotDAO.saveAll(slots);
		serviceDAO.save(service);
	}

	 
	public List<ServiceTypeDto> getServiceTypes() {
		return serviceTypeDAO.findAll().stream().map(ServiceTypeDto::new).toList();
	}


    public List<ServiceSlotsDto> getServiceSlots(Long idRestaurant, Date date) {
		List<Service> services =serviceDAO.findServicesByRestaurant(idRestaurant);
		List<ServiceSlotsDto> servicesSlotsDto = new ArrayList<ServiceSlotsDto>();
		for (Service service : services) {
			servicesSlotsDto.add(new ServiceSlotsDto(service));
		}
		return servicesSlotsDto;
    }


	 
	public List<ServiceDto> getDayServices(Restaurant restaurant, Date date) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getDayServices'");
	}

	public ServiceDTO findById(Long serviceId) {
		return new ServiceDTO(serviceDAO.findById(serviceId).get());
	}

}
