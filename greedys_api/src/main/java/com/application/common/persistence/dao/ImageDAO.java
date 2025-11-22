package com.application.common.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.common.persistence.model.Image;


@Repository
public interface ImageDAO extends JpaRepository<Image, Long>{
	Image findByName(String text);
	Image findFirstByName(String text);

}
