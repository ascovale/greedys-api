package com.application.common.persistence.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.application.common.persistence.model.systemconfig.SetupConfig;

public interface SetupConfigDAO extends JpaRepository<SetupConfig, Long> {
}
