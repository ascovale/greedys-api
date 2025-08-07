package com.application.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Configurazione specifica per i test
 */
@TestConfiguration
@Profile("test")
public class TestConfigurationTestConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa")
                .password("password")
                .driverClassName("org.h2.Driver")
                .build();
    }
}
