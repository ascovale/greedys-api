package com.application.restaurant.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.application.common.mapper.Mapper.Weekday;
import com.application.common.persistence.model.reservation.Slot;
import com.application.common.web.dto.get.SlotDTO;
import com.application.restaurant.dao.RUserDAO;
import com.application.restaurant.dao.ServiceDAO;
import com.application.restaurant.dao.SlotDAO;
import com.application.restaurant.web.post.NewSlotDTO;
import com.application.restaurant.web.post.RestaurantNewSlotDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SlotService {

    private final SlotDAO slotDAO;
    private final ServiceDAO serviceDao;
    private final RUserDAO RUserDao;

    public Set<Weekday> getAvailableDays(Long idService) {
        return slotDAO.findByService_Id(idService).stream()
                .map(Slot::getWeekday)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void addSlot(NewSlotDTO slotDto) {
        com.application.common.persistence.model.reservation.Service service = serviceDao.findById(slotDto.getServiceId())
            .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + slotDto.getServiceId()));
        
        Slot slot = new Slot();
        slot.setStart(slotDto.getStart());
        slot.setEnd(slotDto.getEnd());
        slot.setWeekday(slotDto.getWeekday());
        slot.setService(service);
        slotDAO.save(slot);
    }

    @Transactional
    public void addSlot(Long idRUser, RestaurantNewSlotDTO slotDto) {
        Long idRestaurant = RUserDao.findById(idRUser)
            .orElseThrow(() -> new IllegalArgumentException("RUser not found with id: " + idRUser))
            .getRestaurant().getId();
            
        com.application.common.persistence.model.reservation.Service service = serviceDao.findById(slotDto.getServiceId())
            .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + slotDto.getServiceId()));
            
        if (!service.getRestaurant().getId().equals(idRestaurant)) {
            throw new IllegalArgumentException("Service does not belong to the specified restaurant.");
        }
        
        Slot slot = new Slot();
        slot.setStart(slotDto.getStart());
        slot.setEnd(slotDto.getEnd());
        slot.setWeekday(slotDto.getWeekday());
        slot.setService(service);
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
        return slotDAO.findById(id)
            .map(SlotDTO::new)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + id));
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
