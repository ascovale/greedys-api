package com.application.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.persistence.model.systemconfig.SetupConfig;

public interface SetupConfigDAO extends JpaRepository<SetupConfig, Long> {
}