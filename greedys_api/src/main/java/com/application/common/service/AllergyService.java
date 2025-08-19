package com.application.common.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.mapper.AllergyMapper;
import com.application.common.web.dto.customer.AllergyDTO;
import com.application.common.web.dto.customer.NewAllergyDTO;
import com.application.customer.persistence.dao.AllergyDAO;
import com.application.customer.persistence.model.Allergy;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AllergyService {

    private final AllergyDAO allergyDAO;
	private final AllergyMapper allergyMapper;

    public List<AllergyDTO> getAllAllergies() {
        return allergyDAO.findAll().stream()
                .map(allergyMapper::toDTO)
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
	public AllergyDTO createAllergy(NewAllergyDTO allergyDto) {
		Allergy allergy = new Allergy();
		allergy.setName(allergyDto.getName());
		allergy.setDescription(allergyDto.getDescription());
		allergyDAO.save(allergy);
		return allergyMapper.toDTO(allergy);
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

	@Transactional
	public AllergyDTO modifyAllergyAndReturn(Long idAllergy, NewAllergyDTO allergyDto) {
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		if (allergyDto.getName() != null) {
			allergy.setName(allergyDto.getName());
		}
		if (allergyDto.getDescription() != null) {
			allergy.setDescription(allergyDto.getDescription());
		}
		Allergy savedAllergy = allergyDAO.save(allergy);
		return allergyMapper.toDTO(savedAllergy);
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
