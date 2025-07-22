package com.application.admin.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.application.admin.model.Admin;


@Repository
public interface AdminDAO extends  JpaRepository<Admin, Long>{

	public Admin findByEmail(String email);

}