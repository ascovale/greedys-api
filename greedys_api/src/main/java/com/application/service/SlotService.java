package com.application.service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.application.mapper.Mapper.Weekday;
import com.application.persistence.dao.restaurant.ServiceDAO;
import com.application.persistence.dao.restaurant.SlotDAO;
import com.application.persistence.model.reservation.Slot;
import com.application.web.dto.get.SlotDTO;
import com.application.web.dto.post.NewSlotDTO;

@org.springframework.stereotype.Service
public class SlotService {

    @Autowired
    private SlotDAO slotDAO;

    @Autowired
    private ServiceDAO serviceDao;

    public Set<Weekday> getAvailableDays(Long idService) {
        return slotDAO.findByService_Id(idService).stream()
                .map(Slot::getWeekday)
                .collect(Collectors.toSet());
    }

    public void addSlot(NewSlotDTO slotDto) {
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
}
