package com.application.config;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.application.common.service.FirebaseService;

/**
 * Configurazione specifica per i test
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
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

    @Bean
    @Primary
    public FirebaseService mockFirebaseService() {
        FirebaseService mockService = mock(FirebaseService.class);
        // Mock delle chiamate Firebase per evitare l'inizializzazione delle credenziali Google
        doNothing().when(mockService).sendNotification(anyString(), anyString(), anyMap(), anyList());
        return mockService;
    }
}
