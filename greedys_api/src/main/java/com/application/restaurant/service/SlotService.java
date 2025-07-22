package com.application.restaurant.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.application.common.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Service;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.get.SlotDTO;
import com.application.restaurant.dao.RUserDAO;
import com.application.restaurant.dao.ServiceDAO;
import com.application.restaurant.dao.SlotDAO;
import com.application.restaurant.web.post.NewSlotDTO;
import com.application.restaurant.web.post.RestaurantNewSlotDTO;

import jakarta.transaction.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class SlotService {

    @Autowired
    private SlotDAO slotDAO;

    @Autowired
    private ServiceDAO serviceDao;
    @Autowired
    private RUserDAO RUserDao;

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
    public void addSlot(Long idRUser, RestaurantNewSlotDTO slotDto) {
        Long idRestaurant = RUserDao.findById(idRUser).get().getRestaurant().getId();
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
