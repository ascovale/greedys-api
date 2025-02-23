package com.application.persistence.dao.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.persistence.model.admin.Admin;


@Repository
public interface AdminDAO extends  JpaRepository<Admin, Long>{

	public Admin findByEmail(String email);

}