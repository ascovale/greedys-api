package com.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.restaurant.RestaurantUserDAO;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.model.reservation.Service;
import com.application.persistence.model.reservation.Slot;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.NewSlotDTO;
import com.application.web.dto.post.restaurant.RestaurantNewSlotDTO;

import jakarta.transaction.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class SlotService {

    @Autowired
    private SlotDAO slotDAO;

    @Autowired
    private ServiceDAO serviceDao;
    @Autowired
    private RestaurantUserDAO restaurantUserDao;

    public Set<Weekday> getAvailableDays(Long idService) {
        return slotDAO.findByService_Id(idService).stream()
                .map(Slot::getWeekday)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void addSlot(NewSlotDTO slotDto) {
        Service service = serviceDao.findById(slotDto.getServiceId()).get();
        if (service == null) {
            throw new IllegalArgumentException("Service not found.");
        }
        Slot slot = new Slot();
        slot.setStart(slotDto.getStart());
        slot.setEnd(slotDto.getEnd());
        slot.setWeekday(slotDto.getWeekday());
        slot.setService(service);
        slotDAO.save(slot);
    }

    @Transactional
    public void addSlot(Long idRestaurantUser, RestaurantNewSlotDTO slotDto) {
        Long idRestaurant = restaurantUserDao.findById(idRestaurantUser).get().getRestaurant().getId();
        Service service = serviceDao.findById(slotDto.getServiceId()).get();
        if (service == null) {
            throw new IllegalArgumentException("Service not found.");
        }
        if (!service.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Service does not belong to the specified restaurant.");
        }
        Slot slot = new Slot();
        slot.setStart(slotDto.getStart());
        slot.setEnd(slotDto.getEnd());
        slot.setWeekday(slotDto.getWeekday());
        slot.setService(serviceDao.findById(slotDto.getServiceId()).get());
        slotDAO.save(slot);
    }

    public Collection<SlotDTO> findByService_Id(Long param) {
        return slotDAO.findByService_Id(param).stream()
                .map(SlotDTO::new)
                .collect(Collectors.toList());
    }

    public Collection<Slot> findAll() {
        return slotDAO.findAll();
    }

    public SlotDTO findById(Long id) {
        return new SlotDTO(slotDAO.findById(id).get());
    }

    public boolean cancelSlot(Long id, Long slotId) {
        Slot slot = slotDAO.findById(slotId).orElseThrow(() -> new IllegalArgumentException("Slot not found."));
        if (!slot.getService().getId().equals(id)) {
            throw new IllegalArgumentException("Slot does not belong to the specified service.");
        }
        slotDAO.delete(slot);
        return true;
    }
    public List<SlotDTO> findAllSlots() {
        return slotDAO.findAll().stream()
                .map(SlotDTO::new)
                .collect(Collectors.toList());
    }
    public List<SlotDTO> findSlotsByRestaurantId(Long restaurantId) {
        return slotDAO.findSlotsByRestaurantId(restaurantId).stream()
                .map(SlotDTO::new)
                .collect(Collectors.toList());
    }

}
