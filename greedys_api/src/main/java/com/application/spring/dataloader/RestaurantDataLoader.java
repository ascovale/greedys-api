package com.application.spring.dataloader;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.restaurant.RestaurantDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.ServiceTypeDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.ServiceType;
import com.application.persistence.model.reservation.Slot;
import com.application.persistence.model.restaurant.Restaurant;
import com.application.service.RestaurantService;
import com.application.web.dto.post.NewRestaurantDTO;

@Component
public class RestaurantDataLoader {

    @Autowired
    private RestaurantDAO restaurantDAO;
    @Autowired
    private ServiceTypeDAO serviceTypeDAO;
    @Autowired
    private ServiceDAO serviceDAO;
    @Autowired
    private SlotDAO slotDAO;
    @Autowired
    private RestaurantService restaurantService;

    private static final Logger logger = LoggerFactory.getLogger(RestaurantDataLoader.class);

    @Transactional
    public void createRestaurantLaSoffittaRenovatio() {
        logger.info(">>> --- Creating Restaurant La Soffitta Renovatio --- <<<");
        Restaurant restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        if (restaurant != null) {
            logger.info("Restaurant with name La Soffitta Renovatio already exists.");
            return;
        }

        NewRestaurantDTO restaurantDto = new NewRestaurantDTO();
        restaurantDto.setName("La Soffitta Renovatio");
        restaurantDto.setAddress("Piazza del Risorgimento 46A");
        restaurantDto.setEmail("info@lasoffittarenovatio.it");
        restaurantDto.setPassword("Minosse100%");
        restaurantService.registerRestaurant(restaurantDto);

        restaurant = restaurantDAO.findByName("La Soffitta Renovatio");
        restaurant.setStatus(Restaurant.Status.ENABLED);
        restaurantDAO.save(restaurant);

        ServiceType pranzoType = serviceTypeDAO.findByName("Lunch");
        Service pranzo = new Service();
        pranzo.addServiceType(pranzoType);
        pranzo.setValidFrom(LocalDate.now());
        pranzo.setValidTo(LocalDate.now());
        pranzo.setRestaurant(restaurant);
        serviceDAO.save(pranzo);
        createSlotsForService(pranzo, LocalTime.of(11, 0), LocalTime.of(17, 0));

        ServiceType cenaType = serviceTypeDAO.findByName("Dinner");
        Service cena = new Service();
        cena.addServiceType(cenaType);
        cena.setValidFrom(LocalDate.now());
        cena.setValidTo(LocalDate.now());
        cena.setRestaurant(restaurant);
        serviceDAO.save(cena);
        createSlotsForService(cena, LocalTime.of(17, 30), LocalTime.of(23, 0));
    }

    private List<Slot> createSlotsForService(Service service, LocalTime startTime, LocalTime endTime) {
        List<Slot> slots = new java.util.ArrayList<>();
        LocalTime time = startTime;
        while (time.isBefore(endTime)) {
            for (int day = 1; day <= 7; day++) {
                Weekday weekday = Weekday.values()[day - 1];
                Slot slot = new Slot(time, time.plusMinutes(30));
                slot.setService(service);
                slot.setWeekday(weekday);
                slots.add(slot);
            }
            time = time.plusMinutes(30);
        }
        for (Slot slot : slots) {
            slotDAO.save(slot);
        }
        return slots;
    }
}
