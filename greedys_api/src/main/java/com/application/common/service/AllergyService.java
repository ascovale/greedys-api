package com.application.common.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.web.dto.get.AllergyDTO;
import com.application.common.web.dto.post.NewAllergyDTO;
import com.application.customer.dao.AllergyDAO;
import com.application.customer.model.Allergy;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class AllergyService {

    private final AllergyDAO allergyDAO;

    public AllergyService(AllergyDAO allergyDAO) {
        this.allergyDAO = allergyDAO;
    }

    public List<AllergyDTO> getAllAllergies() {
        return allergyDAO.findAll().stream()
                .map(AllergyDTO::new)
                .collect(Collectors.toList());
    }

    public List<AllergyDTO> getPaginatedAllergies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Allergy> allergies = allergyDAO.findAll(pageable);
        return allergies.stream()
                .map(AllergyDTO::new)
                .collect(Collectors.toList());
    }
    
	@Transactional
	public void createAllergy(NewAllergyDTO allergyDto) {
		Allergy allergy = new Allergy();
		allergy.setName(allergyDto.getName());
		allergy.setDescription(allergyDto.getDescription());
		allergyDAO.save(allergy);
	}

	public void deleteAllergy(Long idAllergy) {
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		allergyDAO.delete(allergy);
	}

	@Transactional
	public void modifyAllergy(Long idAllergy, NewAllergyDTO allergyDto) {
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		if (allergyDto.getName() != null) {
			allergy.setName(allergyDto.getName());
		}
		if (allergyDto.getDescription() != null) {
			allergy.setDescription(allergyDto.getDescription());
		}
		allergyDAO.save(allergy);
	}

    public AllergyDTO getAllergyById(Long allergyId) {
        Allergy allergy = allergyDAO.findById(allergyId)
            .orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
        return new AllergyDTO(allergy);
    }

	public Allergy findByName(String name) {
		return allergyDAO.findByName(name).orElse(null);
	}
}
