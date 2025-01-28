package com.application.persistence.dao.user;

import com.application.persistence.model.user.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AllergyDAO extends JpaRepository<Allergy,Long>{
}