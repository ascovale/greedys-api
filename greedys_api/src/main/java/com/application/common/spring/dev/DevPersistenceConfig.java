package com.application.common.spring.dev;

import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

@Configuration
@Profile({"dev", "dev-minimal"}) // 🚀 Aggiunto support per profile dev-minimal
@EnableJpaRepositories(basePackages = {
    "com.application.admin.persistence.dao",
    "com.application.customer.persistence.dao",
    "com.application.restaurant.persistence.dao",
    "com.application.common.persistence.dao"
})
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "devAuditorProvider")
@RequiredArgsConstructor
public class DevPersistenceConfig {

    private final Environment env;

    @Bean
    DataSource dataSource() {
        System.out.println("🚀 DevPersistenceConfig - Configurazione H2 per sviluppo");
        
        // Configurazione H2 semplificata
        DataSource dataSource = DataSourceBuilder.create()
                .url(env.getProperty("spring.datasource.url"))
                .username(env.getProperty("spring.datasource.username"))
                .password(env.getProperty("spring.datasource.password"))
                .driverClassName(env.getProperty("spring.datasource.driverClassName"))
                .build();
        
        System.out.println("✅ DataSource H2 configurato per sviluppo");
        return dataSource;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
            "com.application.admin.persistence.model",
            "com.application.customer.persistence.model",
            "com.application.restaurant.persistence.model",
            "com.application.common.persistence.model"
        );

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        // Impostiamo valori hardcoded per dev profile per evitare problemi di caricamento
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        
        // 🔧 Fix per H2: Configurazioni per evitare problemi di ordine tabelle
        properties.setProperty("hibernate.hbm2ddl.create_namespaces", "true");
        properties.setProperty("hibernate.connection.autocommit", "false");
        properties.setProperty("hibernate.jdbc.batch_size", "0"); // Disabilita batch per dev
        
        return properties;
    }

    @Bean("devAuditorProvider")
    AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(authentication -> authentication.getName())
                .or(() -> Optional.of("dev-user"));
    }
}
