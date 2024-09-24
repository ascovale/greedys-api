package com.application.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.Image;


@Repository
public interface ImageRepository extends JpaRepository<Image, Long>{
	Image findByName(String text);
	Image findFirstByName(String text);

}
